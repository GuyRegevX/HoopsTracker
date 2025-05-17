package hoops.api.repositories.teams;

import hoops.api.config.TestTimescaleDBConfig;
import hoops.api.models.entities.teams.Team;
import hoops.api.models.entities.teams.TeamStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {TestTimescaleDBConfig.class, TeamsRepositoryImpl.class})
@Testcontainers
@ActiveProfiles("test")
class TeamsRepositoryImplIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(TeamsRepositoryImplIntegrationTest.class);

    @Autowired
    private TeamsRepositoryImpl teamsRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String KNOWN_TEAM_ID = "1"; // Lakers
    private static final String KNOWN_SEASON_ID = "1";
    private static final String KNOWN_TEAM_NAME = "Los Angeles Lakers";
    private static final String KNOWN_LEAGUE_ID = "1"; // NBA

    @BeforeEach
    void setup() {
        // Refresh materialized views to ensure stats are up-to-date for testing
        jdbcTemplate.execute("CALL refresh_continuous_aggregate('team_avg_stats_view', NULL, NULL)");
    }

    @Test
    void getAllTeams_ShouldReturnTeamsFromDatabase() {
        log.info("Testing getAllTeams");
        // When
        List<Team> teams = teamsRepository.getAllTeams();

        // Then
        assertNotNull(teams);
        assertTrue(teams.size() >= 2, "Should return at least 2 teams");

        // Find Lakers in the results
        Team lakers = teams.stream()
                .filter(t -> t.getTeamId().equals(KNOWN_TEAM_ID))
                .findFirst()
                .orElse(null);

        assertNotNull(lakers, "Lakers team should be found");
        assertEquals(KNOWN_TEAM_NAME, lakers.getName());
        assertEquals(KNOWN_LEAGUE_ID, lakers.getLeagueId());
        assertEquals("Western", lakers.getConference());
        assertEquals("Pacific", lakers.getDivision());
        assertEquals("USA", lakers.getCountry());
        assertEquals("Los Angeles", lakers.getCity());
        assertNotNull(lakers.getLastUpdated());
        log.info("Successfully verified team metadata");
    }

    @Test
    void getTeamStats_ShouldReturnAggregatedStatsFromMaterializedView() {
        log.info("Testing getTeamStats returns stats from materialized view");
        // When
        TeamStats stats = teamsRepository.getTeamStats(KNOWN_TEAM_ID, KNOWN_SEASON_ID);

        // Then
        assertNotNull(stats);
        assertEquals(KNOWN_TEAM_ID, stats.getTeamId());
        assertEquals(KNOWN_SEASON_ID, stats.getSeasonId());

        // Based on our test data in 02_test_data.sql, Lakers have:
        // - Players LeBron and Davis who scored in games 1 and 2
        assertTrue(stats.getGames() > 0, "Should have games played");
        assertTrue(stats.getPpg() > 0, "Should have points per game");

        assertNotNull(stats.getLastUpdated(), "Last updated should be set");
        log.info("Successfully verified team stats from materialized view");
    }

    @Test
    void getTeamStats_WhenTeamNotFound_ShouldReturnNull() {
        log.info("Testing getTeamStats for non-existent team");
        // When
        String nonExistentTeamId = "999";
        String nonExistentSeasonId = "998";
        TeamStats stats = teamsRepository.getTeamStats(nonExistentTeamId, nonExistentSeasonId);

        // Then
        assertNull(stats, "Should return null for non-existent team or season");
        log.info("Successfully verified null response for non-existent team");
    }

    @Test
    void addNewTeamAndVerifyRetrieval() {
        // Given
        String newTeamId = "test-" + UUID.randomUUID().toString().substring(0, 8);
        String newTeamName = "Test Team";
        String division = "Central";
        String conference = "Eastern";
        String city = "Test City";
        String country = "USA";

        // When - Insert a new team
        String insertSql = """
            INSERT INTO teams (team_id, name, league_id, country, city, division, conference)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        jdbcTemplate.update(insertSql, newTeamId, newTeamName, KNOWN_LEAGUE_ID, country, city, division, conference);
        log.info("Inserted new test team with ID: {}", newTeamId);

        // Then - Verify the team is retrievable
        List<Team> teams = teamsRepository.getAllTeams();
        Team newTeam = teams.stream()
                .filter(t -> t.getTeamId().equals(newTeamId))
                .findFirst()
                .orElse(null);

        assertNotNull(newTeam, "Newly inserted team should be found");
        assertEquals(newTeamName, newTeam.getName());
        assertEquals(KNOWN_LEAGUE_ID, newTeam.getLeagueId());
        assertEquals(division, newTeam.getDivision());
        assertEquals(conference, newTeam.getConference());
        assertEquals(city, newTeam.getCity());
        assertEquals(country, newTeam.getCountry());

        // Cleanup is handled by @Transactional rolling back after test
    }
}