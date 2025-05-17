package hoops.processor.repositories.playerStatEvents;

import hoops.common.enums.StatType;
import hoops.processor.config.DataSourceConfig;
import hoops.processor.config.JdbcTemplateConfig;
import hoops.processor.config.TestTimescaleDBConfig;
import hoops.processor.models.entities.PlayerStatEvent;
import hoops.processor.repositories.PlayerStatEvents.PlayerStatEventsRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = {
        PlayerStatEventsRepositoryImpl.class,
        DataSourceConfig.class,
        JdbcTemplateConfig.class
})
@Import(TestTimescaleDBConfig.class)
@Testcontainers
@ActiveProfiles("test")
class PlayerStatEventsRepositoryImplTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlayerStatEventsRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        // Clean up the database before each test
        jdbcTemplate.execute("TRUNCATE TABLE player_stat_events CASCADE");
    }

    @Test
    void save_Success() {
        // Arrange
        long expectedVersion = 10;
        PlayerStatEvent event = PlayerStatEvent.builder()
            // No eventId - will be auto-generated
            .version(expectedVersion)
            .playerId("2")
            .gameId("1")
            .teamId("1")
            .seasonId("1")
            .statType(StatType.POINT)
            .statValue(2.0)
            .build();
    
        // Act
        repository.save(event);
        
        // After save, event should have an ID
        assertNotNull(event.getEventId(), "Event ID should be populated after save");
        
        // Check version value directly in database
        Long storedVersion = jdbcTemplate.queryForObject(
                "SELECT version FROM player_stat_events WHERE event_id = ? LIMIT 1",
                Long.class,
                event.getEventId()
        );
        assertEquals(expectedVersion, storedVersion, "Version should match the provided value");
    
        PlayerStatEvent savedEvent = jdbcTemplate.query(
                "SELECT * FROM player_stat_events WHERE event_id = ?",
                ps -> ps.setInt(1, event.getEventId()),
                (rs, rowNum) -> PlayerStatEvent.builder()
                        .eventId(rs.getInt("event_id"))
                        .playerId(rs.getString("player_id"))
                        .gameId(rs.getString("game_id"))
                        .teamId(rs.getString("team_id"))
                        .seasonId(rs.getString("season_id"))
                        .statType(StatType.fromString(rs.getString("stat_type")))
                        .statValue(rs.getDouble("stat_value"))
                        .version(rs.getLong("version"))
                        .build()
        ).stream().findFirst().orElse(null);
    
        // Assert
        assertNotNull(savedEvent);
        assertEquals(event.getEventId(), savedEvent.getEventId());
        assertEquals(expectedVersion, savedEvent.getVersion(), "Version should be correctly stored and retrieved");
        assertEquals(event.getPlayerId(), savedEvent.getPlayerId());
        assertEquals(event.getTeamId(), savedEvent.getTeamId());
        assertEquals(event.getSeasonId(), savedEvent.getSeasonId());
        assertEquals(event.getStatType(), savedEvent.getStatType());
        assertEquals(event.getStatValue(), savedEvent.getStatValue());
    }
} 