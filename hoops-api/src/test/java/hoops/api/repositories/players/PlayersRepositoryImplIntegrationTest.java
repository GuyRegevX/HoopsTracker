package hoops.api.repositories.players;

import hoops.api.config.TestTimescaleDBConfig;
import hoops.api.models.entities.players.Player;
import hoops.api.models.entities.players.PlayerStats;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {PlayersRepositoryImpl.class})
@Import(TestTimescaleDBConfig.class)
@Testcontainers
@ActiveProfiles("test")
class PlayersRepositoryImplIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(PlayersRepositoryImplIntegrationTest.class);

    @Autowired
    private PlayersRepositoryImpl playersRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String KNOWN_PLAYER_ID = "1";
    private static final String KNOWN_SEASON_ID = "1";
    private static final String KNOWN_TEAM_ID = "1";
    private static final String KNOWN_PLAYER_NAME = "LeBron James";
    private static final String KNOWN_TEAM_NAME = "Los Angeles Lakers";

    @Test
    void getAllPlayers_ShouldReturnPlayersFromDatabase() {
        log.info("Testing getAllPlayers database integration");
        // Given
        int expectedPlayerCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM players", Integer.class);

        // When
        List<Player> players = playersRepository.getAllPlayers();

        // Then
        assertNotNull(players);
        assertEquals(expectedPlayerCount, players.size());

        Player player = players.get(0);
        assertEquals(KNOWN_PLAYER_NAME, player.getName());
        assertEquals(KNOWN_PLAYER_ID, player.getPlayerId());
        assertEquals(KNOWN_TEAM_ID, player.getTeamId());
        assertEquals(KNOWN_TEAM_NAME, player.getTeamName());
        assertEquals(23, player.getJerseyNumber());
        assertEquals("SF", player.getPosition());
        assertNotNull(player.getLastUpdated());
        log.info("Successfully verified player data from database");
    }

    @Test
    void getPlayerStats_ShouldAggregateStatsFromMultipleGames() {
        log.info("Testing getPlayerStats aggregates data from multiple games");
        // Given
        

        boolean playerExists = jdbcTemplate.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM players WHERE player_id = ?)",
            Boolean.class, KNOWN_PLAYER_ID);
        assertTrue(playerExists, "Test player should exist in database");

        int completedGames = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(DISTINCT g.game_id) 
            FROM games g
            JOIN player_stat_events pse ON pse.game_id = g.game_id 
            WHERE pse.player_id = ?::integer 
            AND g.season_id = ?::integer
            AND g.state = 'COMPLETED'
            """,
            Integer.class, KNOWN_PLAYER_ID, KNOWN_SEASON_ID);

        int liveGames = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(DISTINCT g.game_id) 
            FROM games g
            JOIN player_stat_events pse ON pse.game_id = g.game_id 
            WHERE pse.player_id = ?::integer 
            AND g.season_id = ?::integer
            AND g.state = 'IN_PROGRESS'
            """,
            Integer.class, KNOWN_PLAYER_ID, KNOWN_SEASON_ID);

        log.info("Found {} completed games and {} live games", completedGames, liveGames);

        // When
        PlayerStats stats = playersRepository.getPlayerStats(KNOWN_PLAYER_ID, KNOWN_SEASON_ID);

        // Then
        assertNotNull(stats);
        assertEquals(KNOWN_PLAYER_ID, stats.getPlayerId());
        assertEquals(KNOWN_SEASON_ID, stats.getSeasonId());
        assertEquals(completedGames + liveGames, stats.getGames(), "Should include both completed and live games");
        
        // Verify stats are properly aggregated
        assertEquals(125.0, stats.getPpg(), 0.1, "PPG should be average of all games");
        assertEquals(30.0, stats.getApg(), 0.1, "APG should be average of all games");
        assertEquals(50.0, stats.getRpg(), 0.1, "RPG should be average of all games");
        assertEquals(10.0, stats.getSpg(), 0.1, "SPG should be average of all games");
        assertEquals(8.0, stats.getBpg(), 0.1, "BPG should be average of all games");
        assertEquals(11.0, stats.getTopg(), 0.1, "TOPG should be average of all games");
        assertEquals(245.0, stats.getMpg(), 0.1, "MPG should be average of all games");
        
        assertNotNull(stats.getLastUpdated(), "Last updated should be set");
        log.info("Successfully verified stat aggregation from database");
    }

    @Test
    void getPlayerStats_WithInvalidSeasonId_ShouldReturnNull() {
        log.info("Testing getPlayerStats with invalid season ID");
        // When
        String invalidSeasonId = "00000000-0000-0000-0000-999999999998";
        PlayerStats stats = playersRepository.getPlayerStats(KNOWN_PLAYER_ID, invalidSeasonId);

        // Then
        assertNull(stats, "Should return null for invalid season ID");
        log.info("Successfully verified null response for invalid season");
    }

    @Test
    void getPlayerStats_WithNonExistentPlayer_ShouldReturnNull() {
        log.info("Testing getPlayerStats with non-existent player");
        // When
        String nonExistentPlayerId = "00000000-0000-0000-0000-999999999999";
        PlayerStats stats = playersRepository.getPlayerStats(nonExistentPlayerId, KNOWN_SEASON_ID);

        // Then
        assertNull(stats);
        log.info("Successfully verified null response for non-existent player");
    }
} 