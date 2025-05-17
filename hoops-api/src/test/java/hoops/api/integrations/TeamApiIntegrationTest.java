package hoops.api.integrations;

import hoops.api.HoopsApiApplication;
import hoops.api.config.TestRedisConfig;
import hoops.api.config.TestTimescaleDBConfig;
import hoops.api.mappers.TeamMapper;
import hoops.api.models.dtos.teams.TeamMetaDTO;
import hoops.api.models.dtos.teams.TeamStatsDTO;
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
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        classes = {HoopsApiApplication.class, TestTimescaleDBConfig.class, TestRedisConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@Testcontainers
public class TeamApiIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(TeamApiIntegrationTest.class);

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RedisClient redisClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TeamMapper teamMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private String testTeamId;
    private String testSeasonId;
    private String testPlayerId;
    private static final String TEST_LEAGUE_ID = "test-league-1";
    private static final String TEST_TEAM_NAME = "Test Team";

    @BeforeEach
    void setUp() {
        log.info("Setting up test data");

        // Generate unique IDs for this test run
        testTeamId = "test-team-" + UUID.randomUUID().toString().substring(0, 8);
        testSeasonId = "test-season-" + UUID.randomUUID().toString().substring(0, 8);
        testPlayerId = "test-player-" + UUID.randomUUID().toString().substring(0, 8);

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
            String uniqueLeagueId = TEST_LEAGUE_ID;
            String otherTeamId = "test-team-other-" + UUID.randomUUID().toString().substring(0, 8);

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

            // Step 2: Insert teams
            try {
                jdbcTemplate.update(
                        "INSERT INTO teams (team_id, name, league_id, country) VALUES (?, ?, ?, ?)",
                        testTeamId, TEST_TEAM_NAME, uniqueLeagueId, "USA"
                );
                log.info("Created new team with ID: {}", testTeamId);
            } catch (DuplicateKeyException e) {
                log.info("Team with ID {} already exists", testTeamId);
            }

            try {
                jdbcTemplate.update(
                        "INSERT INTO teams (team_id, name, league_id, country) VALUES (?, ?, ?, ?)",
                        otherTeamId, "Other Test Team", uniqueLeagueId, "USA"
                );
                log.info("Created other team with ID: {}", otherTeamId);
            } catch (DuplicateKeyException e) {
                log.info("Team with ID {} already exists", otherTeamId);
            }

            // Step 3: Insert player for the team
            try {
                jdbcTemplate.update(
                        "INSERT INTO players (player_id, name, team_id, jersey_number, position) VALUES (?, ?, ?, ?, ?)",
                        testPlayerId, "Test Player", testTeamId, 23, "F"
                );
                log.info("Created test player with ID: {}", testPlayerId);
            } catch (DuplicateKeyException e) {
                log.info("Player with ID {} already exists", testPlayerId);
            }

            // Step 4: Insert season with check and set as active
            try {
                jdbcTemplate.update(
                        "INSERT INTO seasons (season_id, name, start_date, end_date, active) VALUES (?, ?, ?::date, ?::date, ?)",
                        testSeasonId, "Test Season", "2023-01-01", "2023-12-31", true
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

            // Step 5: Insert game with unique ID
            String testGameId = "test-game-" + UUID.randomUUID().toString().substring(0, 8);
            try {
                jdbcTemplate.update(
                        "INSERT INTO games (game_id, game_date, season_id, league_id, home_team_id, away_team_id, start_time, state) " +
                                "VALUES (?, CURRENT_DATE, ?, ?, ?, ?, '20:00:00', 'COMPLETED')",
                        testGameId, testSeasonId, uniqueLeagueId, testTeamId, otherTeamId
                );
                log.info("Created new game with ID: {}", testGameId);
            } catch (DuplicateKeyException e) {
                log.info("Game with ID {} already exists", testGameId);
            }

            // Step 6: Insert player stat events (which will be aggregated into team stats)
            OffsetDateTime now = OffsetDateTime.now();
            try {
                // Insert points
                jdbcTemplate.update(
                        "INSERT INTO player_stat_events (player_id, game_id, team_id, season_id, stat_type, stat_value, version) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                        testPlayerId, testGameId, testTeamId, testSeasonId, "point", 105.0, 1
                );

                // Insert assists
                jdbcTemplate.update(
                        "INSERT INTO player_stat_events (player_id, game_id, team_id, season_id, stat_type, stat_value, version) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                        testPlayerId, testGameId, testTeamId, testSeasonId, "assist", 22.0, 1
                );

                // Insert rebounds
                jdbcTemplate.update(
                        "INSERT INTO player_stat_events (player_id, game_id, team_id, season_id, stat_type, stat_value, version) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                        testPlayerId, testGameId, testTeamId, testSeasonId, "rebound", 45.0, 1
                );

                // Insert steals
                jdbcTemplate.update(
                        "INSERT INTO player_stat_events (player_id, game_id, team_id, season_id, stat_type, stat_value, version) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                        testPlayerId, testGameId, testTeamId, testSeasonId, "steal", 5.0, 1
                );

                // Insert blocks
                jdbcTemplate.update(
                        "INSERT INTO player_stat_events (player_id, game_id, team_id, season_id, stat_type, stat_value, version) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                        testPlayerId, testGameId, testTeamId, testSeasonId, "block", 3.0, 1
                );

                // Insert turnovers
                jdbcTemplate.update(
                        "INSERT INTO player_stat_events (player_id, game_id, team_id, season_id, stat_type, stat_value, version) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                        testPlayerId, testGameId, testTeamId, testSeasonId, "turnover", 2.0, 1
                );

                log.info("Created player stat events for game {}", testGameId);
            } catch (Exception e) {
                log.error("Error inserting player stats: {}", e.getMessage(), e);
            }

            // Step 7: Refresh materialized view for team stats
            try {
                jdbcTemplate.execute("CALL refresh_continuous_aggregate('team_avg_stats_view', NULL, NULL)");
                log.info("Refreshed team_avg_stats_view materialized view");
            } catch (Exception e) {
                log.warn("Error refreshing team stats materialized view: {}", e.getMessage());
            }

            // Verify data was correctly inserted into materialized view
            try {
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM team_avg_stats_view WHERE team_id = ? AND season_id = ?",
                        Integer.class,
                        testTeamId, testSeasonId
                );
                log.info("Team stats count in materialized view: {}", count);
                if (count == null || count == 0) {
                    log.warn("No team stats found in materialized view after refresh");
                }
            } catch (Exception e) {
                log.error("Error checking materialized view: {}", e.getMessage());
            }

            log.info("Test data setup complete with league={}, team={}, season={}, game={}",
                    uniqueLeagueId, testTeamId, testSeasonId, testGameId);

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
            commands.del(RedisKeyUtil.getTeamStatsKey(testTeamId, testSeasonId));
        } catch (Exception e) {
            log.error("Error cleaning Redis: {}", e.getMessage());
        }
    }

    @Test
    void getTeamById_ShouldReturnSingleTeam() {
        log.info("Starting test: getTeamById_ShouldReturnSingleTeam");

        // Setup is already done in the setUp() method
        String redisKey = RedisKeyUtil.getTeamStatsKey(testTeamId, testSeasonId);

        // Check Redis is empty before the request
        try (var connection = redisClient.connect()) {
            RedisCommands<String, String> commands = connection.sync();
            String cachedValue = commands.get(redisKey);
            assertNull(cachedValue, "Redis should not have team stats cached before API call");
            log.info("Confirmed Redis does not have team stats cached");
        } catch (Exception e) {
            log.error("Error checking Redis before API call: {}", e.getMessage(), e);
            fail("Failed to check Redis: " + e.getMessage());
        }

        // Print the content of the team_avg_stats_view for this team
        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                    "SELECT * FROM team_avg_stats_view WHERE team_id = ? AND season_id = ?",
                    testTeamId, testSeasonId
            );
            log.info("team_avg_stats_view contents for team {}: {}", testTeamId, results);
        } catch (Exception e) {
            log.error("Error querying team_avg_stats_view: {}", e.getMessage());
        }

        // Call API to get a single team by ID
        String getTeamUrl = "http://localhost:" + port + "/api/v1/teams/" + testTeamId + "/stats?seasonId=" + testSeasonId;
        log.info("Getting single team from: {}", getTeamUrl);

        ResponseEntity<TeamStatsDTO> teamResponse = restTemplate.getForEntity(
                getTeamUrl,
                TeamStatsDTO.class
        );

        // Verify response status
        assertEquals(200, teamResponse.getStatusCodeValue(), "HTTP status should be 200");
        TeamStatsDTO teamStatsDTO = teamResponse.getBody();

        // Verify team data
        assertNotNull(teamStatsDTO, "Team should not be null");
        assertEquals(testTeamId, teamStatsDTO.getTeamId(), "Team ID should match");

        // Add checks for average statistics
        assertTrue(teamStatsDTO.getPpg() >= 0, "Points per game should be a non-negative number");
        assertTrue(teamStatsDTO.getRpg() >= 0, "Rebounds per game should be a non-negative number");
        assertTrue(teamStatsDTO.getApg() >= 0, "Assists per game should be a non-negative number");

        // Now check Redis to verify the team stats were cached
        try (var connection = redisClient.connect()) {
            RedisCommands<String, String> commands = connection.sync();
            String cachedValue = commands.get(redisKey);
            assertNotNull(cachedValue, "Redis should have team stats cached after API call");
            log.info("Successfully verified team stats were cached in Redis");

            // Optionally, verify the cached data matches what the API returned
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule()); // For handling Java 8 date/time types
            TeamStatsDTO cachedStats = objectMapper.readValue(cachedValue, TeamStatsDTO.class);
            assertEquals(teamStatsDTO.getTeamId(), cachedStats.getTeamId(), "Cached team ID should match");
            assertEquals(teamStatsDTO.getPpg(), cachedStats.getPpg(), 0.01, "Cached PPG should match");
        } catch (Exception e) {
            log.error("Error checking Redis after API call: {}", e.getMessage(), e);
            fail("Failed to check Redis: " + e.getMessage());
        }

        log.info("Successfully verified single team retrieval and caching: {}", teamStatsDTO.getTeamId());
    }

    @Test
    void getTeamMetaData_ShouldReturnBasicInfo() {
        log.info("Starting test: getTeamMetaData_ShouldReturnBasicInfo");

        // Call API to get team metadata
        String getTeamMetaUrl = "http://localhost:" + port + "/api/v1/teams";
        log.info("Getting team metadata from: {}", getTeamMetaUrl);

        ResponseEntity<List<TeamMetaDTO>> metaResponse = restTemplate.exchange(
                getTeamMetaUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<TeamMetaDTO>>() {}
        );

        // Verify response status
        assertEquals(HttpStatus.OK, metaResponse.getStatusCode(), "HTTP status should be 200");
        List<TeamMetaDTO> teamMetaList = metaResponse.getBody();

        // Verify metadata list
        assertNotNull(teamMetaList, "Team metadata list should not be null");
        assertFalse(teamMetaList.isEmpty(), "Team metadata list should not be empty");

        // Find the specific team by ID
        TeamMetaDTO teamMeta = teamMetaList.stream()
                .filter(team -> team.getTeamId().equals(testTeamId))
                .findFirst()
                .orElse(null);

        // Verify team metadata
        assertNotNull(teamMeta, "Team with ID " + testTeamId + " should be found in the list");
        assertEquals(testTeamId, teamMeta.getTeamId(), "Team ID should match");
        assertEquals(TEST_TEAM_NAME, teamMeta.getName(), "Team name should match");
        assertEquals(TEST_LEAGUE_ID, teamMeta.getLeagueId(), "League ID should match");

        // You may also verify additional fields if your API provides that
        assertNotNull(teamMeta.getLastUpdated(), "Last updated timestamp should be present");

        log.info("Successfully verified team metadata for: {}", teamMeta.getName());
    }

    @Test
    void getTeamByIdNotFound_ShouldReturn404() {
        log.info("Starting test: getTeamByIdNotFound_ShouldReturn404");

        // Generate a random ID that doesn't exist
        String nonExistentTeamId = "non-existent-" + UUID.randomUUID();

        // Call API with non-existent ID
        String getTeamUrl = "http://localhost:" + port + "/api/v1/teams/" + nonExistentTeamId + "/stats?seasonId=" + testSeasonId;
        log.info("Attempting to get non-existent team from: {}", getTeamUrl);

        ResponseEntity<TeamStatsDTO> teamResponse = restTemplate.getForEntity(
                getTeamUrl,
                TeamStatsDTO.class
        );

        // Verify 404 response
        assertEquals(404, teamResponse.getStatusCodeValue(), "HTTP status should be 404 NOT FOUND");

        log.info("Successfully verified 404 response for non-existent team");
    }
}