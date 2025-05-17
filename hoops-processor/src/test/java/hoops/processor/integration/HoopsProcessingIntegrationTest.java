package hoops.processor.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import hoops.common.constants.StreamConstants;
import hoops.common.models.events.PointsEvent;
import hoops.common.enums.StatType;
import hoops.processor.config.TestRedisConfig;
import hoops.processor.config.TestTimescaleDBConfig;
import hoops.processor.consumers.GameEventStreamConsumer;
import hoops.processor.infrastructure.redis.RedisStreamManager;
import hoops.processor.models.entities.PlayerStatEvent;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import({TestRedisConfig.class, TestTimescaleDBConfig.class})
@ActiveProfiles("test")
class HoopsProcessingIntegrationTest {

    @Autowired
    private GameEventStreamConsumer streamConsumer;

    @Autowired
    private RedisStreamManager redisStreamManager;

    @Autowired
    private RedisClient redisClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String STREAM_NAME = StreamConstants.GAME_EVENTS_STREAM;
    private static final String GROUP_NAME = "game-events-processor";
    private String seasonId;
    private RedisCommands<String, String> redisCommands;

    @BeforeEach
    void setUp() {
        // Get Redis connection
        redisCommands = redisClient.connect().sync();

        // Clear Redis and DB
        redisCommands.flushall();


        // Clean up the database tables in the correct order (respect foreign key constraints)
        jdbcTemplate.execute("TRUNCATE TABLE player_stat_events CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE games CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE players CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE teams CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE seasons CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE leagues CASCADE");
    
        // Create a league
        String leagueId = "NBA";
        jdbcTemplate.update("""
            INSERT INTO leagues (league_id, name, country)
            VALUES (?, ?, ?)
            """,
            leagueId,
            "National Basketball Association",
            "USA"
        );
    
        // Create active season directly in the database
        seasonId = UUID.randomUUID().toString();
        jdbcTemplate.update("""
            INSERT INTO seasons (season_id, name, start_date, end_date, active)
            VALUES (?, ?, '2023-10-01', '2024-06-30', true)
            """,
            seasonId,
            "2023-2024"
        );
        
        // Create teams
        String teamId = "BOS";
        jdbcTemplate.update("""
            INSERT INTO teams (team_id, name, league_id, country, city, division, conference)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            teamId,
            "Boston Celtics",
            leagueId,
            "USA",
            "Boston",
            "Atlantic",
            "Eastern"
        );
        
        // Create another team for the opponent
        String opponentTeamId = "LAL";
        jdbcTemplate.update("""
            INSERT INTO teams (team_id, name, league_id, country, city, division, conference)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            opponentTeamId,
            "Los Angeles Lakers",
            leagueId,
            "USA",
            "Los Angeles",
            "Pacific",
            "Western"
        );
        
        // Create player
        String playerId = "jt0";
        jdbcTemplate.update("""
            INSERT INTO players (player_id, name, team_id, jersey_number, position)
            VALUES (?, ?, ?, ?, ?)
            """,
            playerId,
            "Jayson Tatum",
            teamId,
            "0",
            "Forward"
        );
        
        // Create game
        String gameId = "2024030100";
        jdbcTemplate.update("""
            INSERT INTO games (game_id, game_date, season_id, league_id, home_team_id, away_team_id, start_time, state)
            VALUES (?, CURRENT_DATE, ?, ?, ?, ?, '19:30:00', ?)
            """,
            gameId,
            seasonId,
            leagueId,
            teamId,
            opponentTeamId,
            "SCHEDULED"
        );

        // Initialize consumer group
        redisStreamManager.createConsumerGroup(STREAM_NAME, GROUP_NAME);
    }

    @Test
    void shouldProcessEventFromStreamAndSaveToDatabase() throws Exception {
        // Arrange
        PointsEvent event = new PointsEvent();
        event.setVersion(2024030100L);
        event.setGameId("2024030100");
        event.setTeamId("BOS");
        event.setPlayerId("jt0");
        event.setValue(2.0);
        event.setEvent("point");

        String eventJson = objectMapper.writeValueAsString(event);

        // Add event to stream
        redisCommands.xadd(STREAM_NAME, Map.of("data", eventJson));

        // Act
        streamConsumer.processGameEvents();

        // Allow time for processing
        Thread.sleep(1000);

        // Assert
        // 1. Check if message was processed (no pending messages)
       var pending = redisCommands.xpending(
                STREAM_NAME,
                GROUP_NAME
        );
        assertThat(pending.getCount()).isEqualTo(0);

        // 2. Verify event was saved to database using JDBC
        List<PlayerStatEvent> savedEvents = jdbcTemplate.query(
                "SELECT * FROM player_stat_events",
                (rs, rowNum) -> PlayerStatEvent.builder()
                        .playerId(rs.getString("player_id"))
                        .teamId(rs.getString("team_id"))
                        .gameId(rs.getString("game_id"))
                        .statType(StatType.fromString(rs.getString("stat_type")))
                        .statValue(rs.getDouble("stat_value"))
                        .seasonId(rs.getString("season_id"))
                        .build()
        );

        assertThat(savedEvents).hasSize(1);

        PlayerStatEvent savedEvent = savedEvents.get(0);
        assertThat(savedEvent.getPlayerId()).isEqualTo("jt0");
        assertThat(savedEvent.getTeamId()).isEqualTo("BOS");
        assertThat(savedEvent.getGameId()).isEqualTo("2024030100");
        assertThat(savedEvent.getStatType().getValue()).isEqualTo("point");
        assertThat(savedEvent.getStatValue()).isEqualTo(2.0);
        assertThat(savedEvent.getSeasonId()).isEqualTo(seasonId);
    }

    @Test
    void shouldHandleInvalidEventGracefully() throws Exception {
        // Arrange - Add invalid JSON to stream
        redisCommands.xadd(STREAM_NAME, Map.of("data", "invalid json"));

        // Act
        streamConsumer.processGameEvents();
        Thread.sleep(1000);

        // Assert - No events should be saved to database
        int count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM player_stat_events",
                Integer.class
        );
        assertThat(count).isZero();
    }
}