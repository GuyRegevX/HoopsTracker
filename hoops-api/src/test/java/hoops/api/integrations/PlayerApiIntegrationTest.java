package hoops.api.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import hoops.api.HoopsApiApplication;
import hoops.api.config.TestRedisConfig;
import hoops.api.config.TestTimescaleDBConfig;
import hoops.api.mappers.PlayerMapper;
import hoops.api.models.dtos.players.PlayerMetaDTO;
import hoops.api.models.dtos.players.PlayerStatsDTO;
import hoops.common.redis.RedisKeyUtil;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        classes = {HoopsApiApplication.class, TestTimescaleDBConfig.class, TestRedisConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@Testcontainers
public class PlayerApiIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(PlayerApiIntegrationTest.class);

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RedisClient redisClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlayerMapper playerMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private String testPlayerId;
    private String testSeasonId;
    private static final String TEST_TEAM_ID = "test-team-1";
    private static final String TEST_PLAYER_NAME = "Test Player";

    @BeforeEach
    void setUp() {
        log.info("Setting up test data");
        // Initialize database schema
        try {
            // Load schema from resources
            jdbcTemplate.execute(getClass().getClassLoader().getResourceAsStream("01_schema.sql").toString());
        } catch (Exception e) {
            log.warn("Error loading schema, may already exist: {}", e.getMessage());
        }

        // Generate unique IDs for this test run
        testPlayerId = "test-player-" + UUID.randomUUID().toString().substring(0, 8);
        testSeasonId = "test-season-" + UUID.randomUUID().toString().substring(0, 8);

        // Set up required test data
        setupTestData();

        // Clean Redis before test
        try (var connection = redisClient.connect()) {
            RedisCommands<String, String> commands = connection.sync();
            commands.flushall();
            log.info("Redis cache cleared");
        } catch (Exception e) {
            log.error("Error clearing Redis: {}", e.getMessage());
        }
    }

    private void setupTestData() {
        try {
            // Generate unique IDs for this test run to avoid conflicts
            String uniqueLeagueId = "test-league-" + UUID.randomUUID().toString().substring(0, 8);
            String uniqueTeamId = TEST_TEAM_ID; // Keep using the constant for reference in other parts of the test

            log.info("Setting up test data with unique league ID: {}", uniqueLeagueId);

            // Step 1: Insert league with check
            boolean leagueExists = false;
            try {
                Integer leagueCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM leagues WHERE league_id = ?",
                        Integer.class,
                        uniqueLeagueId);
                leagueExists = (leagueCount != null && leagueCount > 0);
            } catch (Exception e) {
                log.warn("Error checking if league exists: {}", e.getMessage());
            }

            if (!leagueExists) {
                try {
                    jdbcTemplate.update(
                            "INSERT INTO leagues (league_id, name, country) VALUES (?, ?, ?)",
                            uniqueLeagueId, "Test League", "Test Country"
                    );
                    log.info("Created new league with ID: {}", uniqueLeagueId);
                } catch (DuplicateKeyException e) {
                    log.info("League with ID {} already exists", uniqueLeagueId);
                }
            }

            // Step 2: Insert team with check
            boolean teamExists = false;
            try {
                Integer teamCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM teams WHERE team_id = ?",
                        Integer.class,
                        uniqueTeamId);
                teamExists = (teamCount != null && teamCount > 0);
            } catch (Exception e) {
                log.warn("Error checking if team exists: {}", e.getMessage());
            }

            if (!teamExists) {
                try {
                    jdbcTemplate.update(
                            "INSERT INTO teams (team_id, name, league_id, country) VALUES (?, ?, ?, ?)",
                            uniqueTeamId, "Test Team", uniqueLeagueId, "USA"
                    );
                    log.info("Created new team with ID: {}", uniqueTeamId);
                } catch (DuplicateKeyException e) {
                    log.info("Team with ID {} already exists", uniqueTeamId);
                }
            }

            // Step 3: Insert player with check
            boolean playerExists = false;
            try {
                Integer playerCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM players WHERE player_id = ?",
                        Integer.class,
                        testPlayerId);
                playerExists = (playerCount != null && playerCount > 0);
            } catch (Exception e) {
                log.warn("Error checking if player exists: {}", e.getMessage());
            }

            if (!playerExists) {
                try {
                    jdbcTemplate.update(
                            "INSERT INTO players (player_id, name, team_id, jersey_number, position) VALUES (?, ?, ?, ?, ?)",
                            testPlayerId, TEST_PLAYER_NAME, uniqueTeamId, "23", "G"
                    );
                    log.info("Created new player with ID: {}", testPlayerId);
                } catch (DuplicateKeyException e) {
                    log.info("Player with ID {} already exists", testPlayerId);
                }
            }

            // Step 4: Insert season with check and set as active
            boolean seasonExists = false;
            try {
                Integer seasonCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM seasons WHERE season_id = ?",
                        Integer.class,
                        testSeasonId);
                seasonExists = (seasonCount != null && seasonCount > 0);
            } catch (Exception e) {
                log.warn("Error checking if season exists: {}", e.getMessage());
            }

            if (!seasonExists) {
                try {
                    jdbcTemplate.update(
                            "INSERT INTO seasons (season_id, name, start_date, end_date, active) VALUES (?, ?, ?::date, ?::date, ?)",
                            testSeasonId, "Test Season", "2023-01-01", "2023-12-31", true  // Set active to true
                    );
                    log.info("Created new season with ID: {} (active)", testSeasonId);
                } catch (DuplicateKeyException e) {
                    log.info("Season with ID {} already exists", testSeasonId);
                    // Make sure it's active even if it already exists
                    jdbcTemplate.update(
                            "UPDATE seasons SET active = true WHERE season_id = ?",
                            testSeasonId
                    );
                }
            } else {
                // Make sure the existing season is set as active
                jdbcTemplate.update(
                        "UPDATE seasons SET active = true WHERE season_id = ?",
                        testSeasonId
                );
                log.info("Updated existing season {} to be active", testSeasonId);
            }

            // Step 5: Insert game with unique ID
            String testGameId = "test-game-" + UUID.randomUUID().toString().substring(0, 8);
            boolean gameExists = false;
            try {
                Integer gameCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM games WHERE game_id = ?",
                        Integer.class,
                        testGameId);
                gameExists = (gameCount != null && gameCount > 0);
            } catch (Exception e) {
                log.warn("Error checking if game exists: {}", e.getMessage());
            }

            if (!gameExists) {
                try {
                    jdbcTemplate.update(
                            "INSERT INTO games (game_id, game_date, season_id, league_id, home_team_id, away_team_id, start_time, state) " +
                                    "VALUES (?, CURRENT_DATE, ?, ?, ?, ?, '20:00:00', 'COMPLETED')",
                            testGameId, testSeasonId, uniqueLeagueId, uniqueTeamId, uniqueTeamId
                    );
                    log.info("Created new game with ID: {}", testGameId);
                } catch (DuplicateKeyException e) {
                    log.info("Game with ID {} already exists", testGameId);
                }
            }

            // Step 6: Insert player stats (these should be unique per game anyway)
            OffsetDateTime now = OffsetDateTime.now();
            try {
                // Use unique event IDs to avoid conflicts
                int eventIdBase = new Random().nextInt(1000) + 1000; // Random base between 1000-1999

                jdbcTemplate.update(
                        "INSERT INTO player_stat_events (event_id, game_id, player_id, team_id, season_id, stat_type, stat_value, version, created_at) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        eventIdBase, testGameId, testPlayerId, uniqueTeamId, testSeasonId, "point", 30.0, 1, now
                );

                jdbcTemplate.update(
                        "INSERT INTO player_stat_events (event_id, game_id, player_id, team_id, season_id, stat_type, stat_value, version, created_at) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        eventIdBase + 1, testGameId, testPlayerId, uniqueTeamId, testSeasonId, "assist", 8.0, 1, now
                );

                jdbcTemplate.update(
                        "INSERT INTO player_stat_events (event_id, game_id, player_id, team_id, season_id, stat_type, stat_value, version, created_at) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        eventIdBase + 2, testGameId, testPlayerId, uniqueTeamId, testSeasonId, "rebound", 12.0, 1, now
                );

                log.info("Created player stats for game {}", testGameId);
            } catch (Exception e) {
                log.error("Error inserting player stats: {}", e.getMessage());
            }

            // Step 7: Refresh materialized view (this should be safe to run even if it fails)
            try {
                jdbcTemplate.execute("CALL refresh_continuous_aggregate('player_avg_stats_view', NULL, NULL)");
                log.info("Refreshed player_avg_stats_view materialized view");
            } catch (Exception e) {
                log.warn("Error refreshing materialized view: {}", e.getMessage());
            }

            log.info("Test data setup complete with league={}, team={}, player={}, season={}, game={}",
                    uniqueLeagueId, uniqueTeamId, testPlayerId, testSeasonId, testGameId);

        } catch (Exception e) {
            log.error("Error setting up test data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to set up test data", e);
        }
    }

    @AfterEach
    void tearDown() {
        // Clean up created data
        log.info("Cleaning up test resources");
        try (var connection = redisClient.connect()) {
            RedisCommands<String, String> commands = connection.sync();
            commands.del(RedisKeyUtil.getPlayerStatsKey(testPlayerId, testSeasonId));
        } catch (Exception e) {
            log.error("Error cleaning Redis: {}", e.getMessage());
        }
    }

    @Test
    void getPlayerById_ShouldReturnSinglePlayer() {
        log.info("Starting test: getPlayerById_ShouldReturnSinglePlayer");

        // Setup is already done in the setUp() method
        String redisKey = RedisKeyUtil.getPlayerStatsKey(testPlayerId, testSeasonId);

        // Check Redis is empty before the request
        try (var connection = redisClient.connect()) {
            RedisCommands<String, String> commands = connection.sync();
            String cachedValue = commands.get(redisKey);
            assertNull(cachedValue, "Redis should not have player stats cached before API call");
            log.info("Confirmed Redis does not have player stats cached");
        } catch (Exception e) {
            log.error("Error checking Redis before API call: {}", e.getMessage(), e);
            fail("Failed to check Redis: " + e.getMessage());
        }

        // Call API to get a single player by ID
        String getPlayerUrl = "http://localhost:" + port + "/api/v1/players/" + testPlayerId + "/stats?seasonId=" + testSeasonId;
        log.info("Getting single player from: {}", getPlayerUrl);

        ResponseEntity<PlayerStatsDTO> playerResponse = restTemplate.getForEntity(
                getPlayerUrl,
                PlayerStatsDTO.class
        );

        // Verify response status
        assertEquals(200, playerResponse.getStatusCodeValue(), "HTTP status should be 200");
        PlayerStatsDTO playerStatsDTO = playerResponse.getBody();

        // Verify player data
        assertNotNull(playerStatsDTO, "Player should not be null");
        assertEquals(testPlayerId, playerStatsDTO.getPlayerId(), "Player ID should match");
        assertEquals(TEST_TEAM_ID, playerStatsDTO.getTeamId(), "Team ID should match");

        // Add checks for average statistics
        assertTrue(playerStatsDTO.getPpg() >= 0, "Points per game should be a non-negative number");
        assertTrue(playerStatsDTO.getRpg() >= 0, "Rebounds per game should be a non-negative number");
        assertTrue(playerStatsDTO.getApg() >= 0, "Assists per game should be a non-negative number");

        // Now check Redis to verify the player stats were cached
        try (var connection = redisClient.connect()) {
            RedisCommands<String, String> commands = connection.sync();
            String cachedValue = commands.get(redisKey);
            assertNotNull(cachedValue, "Redis should have player stats cached after API call");
            log.info("Successfully verified player stats were cached in Redis");

            // Optionally, verify the cached data matches what the API returned
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule()); // For handling Java 8 date/time types
            PlayerStatsDTO cachedStats = objectMapper.readValue(cachedValue, PlayerStatsDTO.class);
            assertEquals(playerStatsDTO.getPlayerId(), cachedStats.getPlayerId(), "Cached player ID should match");
            assertEquals(playerStatsDTO.getPpg(), cachedStats.getPpg(), 0.01, "Cached PPG should match");
        } catch (Exception e) {
            log.error("Error checking Redis after API call: {}", e.getMessage(), e);
            fail("Failed to check Redis: " + e.getMessage());
        }

        log.info("Successfully verified single player retrieval and caching: {}", playerStatsDTO.getPlayerId());
    }

    @Test
    void getPlayerMetaData_ShouldReturnBasicInfo() {
        log.info("Starting test: getPlayerMetaData_ShouldReturnBasicInfo");

        // Call API to get player metadata
        String getPlayerMetaUrl = "http://localhost:" + port + "/api/v1/players";
        log.info("Getting player metadata from: {}", getPlayerMetaUrl);

        ResponseEntity<List<PlayerMetaDTO>> metaResponse = restTemplate.exchange(
                getPlayerMetaUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<PlayerMetaDTO>>() {
                }
        );

        // Verify response status
        assertEquals(HttpStatus.OK, metaResponse.getStatusCode(), "HTTP status should be 200");
        List<PlayerMetaDTO> playerMetaList = metaResponse.getBody();

        // Verify metadata list
        assertNotNull(playerMetaList, "Player metadata list should not be null");
        assertFalse(playerMetaList.isEmpty(), "Player metadata list should not be empty");

        // Find the specific player by ID
        PlayerMetaDTO playerMeta = playerMetaList.stream()
                .filter(player -> player.getPlayerId().equals(testPlayerId))
                .findFirst()
                .orElse(null);

        // Verify player metadata
        assertNotNull(playerMeta, "Player with ID " + testPlayerId + " should be found in the list");
        assertEquals(testPlayerId, playerMeta.getPlayerId(), "Player ID should match");
        assertEquals(TEST_PLAYER_NAME, playerMeta.getName(), "Player name should match");
        assertEquals(TEST_TEAM_ID, playerMeta.getTeamId(), "Team ID should match");

        // You may also verify additional fields like teamName if your API provides that
        assertNotNull(playerMeta.getLastUpdated(), "Last updated timestamp should be present");


        log.info("Successfully verified player metadata for: {}", playerMeta.getName());

    }

    @Test
    void getPlayerByIdNotFound_ShouldReturn404() {
        log.info("Starting test: getPlayerByIdNotFound_ShouldReturn404");

        // Generate a random ID that doesn't exist
        String nonExistentPlayerId = "non-existent-" + UUID.randomUUID();

        // Call API with non-existent ID
        String getPlayerUrl = "http://localhost:" + port + "/api/v1/players/" + nonExistentPlayerId + "/stats?seasonId=" + testSeasonId;
        log.info("Attempting to get non-existent player from: {}", getPlayerUrl);

        ResponseEntity<PlayerMetaDTO> playerResponse = restTemplate.getForEntity(
                getPlayerUrl,
                PlayerMetaDTO.class
        );

        // Verify 404 response
        assertEquals(404, playerResponse.getStatusCodeValue(), "HTTP status should be 404 NOT FOUND");

        log.info("Successfully verified 404 response for non-existent player");
    }
}