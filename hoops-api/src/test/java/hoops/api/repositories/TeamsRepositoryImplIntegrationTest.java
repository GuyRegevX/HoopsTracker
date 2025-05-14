package hoops.api.repositories;

import hoops.api.config.TestTimescaleDBConfig;
import hoops.api.models.entities.Team;
import hoops.api.models.entities.TeamStats;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {TestTimescaleDBConfig.class, TeamsRepositoryImpl.class})
@Testcontainers
@ActiveProfiles("test")
class TeamsRepositoryImplIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(TeamsRepositoryImplIntegrationTest.class);

    @Autowired
    private TeamsRepositoryImpl teamsRepository;

    private static final String KNOWN_TEAM_ID = "00000000-0000-0000-0000-000000000201";
    private static final String KNOWN_SEASON_ID = "00000000-0000-0000-0000-000000000101";
    private static final String KNOWN_LEAGUE_ID = "00000000-0000-0000-0000-000000000001";
    private static final String KNOWN_TEAM_NAME = "Los Angeles Lakers";
    private static final String KNOWN_LEAGUE_NAME = "NBA";

    @Test
    void getAllTeams_ShouldReturnTeamsFromDatabase() {
        log.info("Testing getAllTeams");
        // When
        List<Team> teams = teamsRepository.getAllTeams();

        // Then
        assertNotNull(teams);
        assertEquals(1, teams.size());

        Team team = teams.get(0);
        assertEquals(KNOWN_TEAM_NAME, team.getName());
        assertEquals(KNOWN_TEAM_ID, team.getTeamId());
        assertEquals(KNOWN_LEAGUE_ID, team.getLeagueId());
        assertEquals(KNOWN_LEAGUE_NAME, team.getLeagueName());
        assertEquals("USA", team.getCountry());
        assertNotNull(team.getLastUpdated());
        log.info("Successfully verified team metadata");
    }

    @Test
    void getTeamStats_ShouldReturnLatestStats() {
        log.info("Testing getTeamStats returns latest stats from live game");
        // When
        TeamStats stats = teamsRepository.getTeamStats(KNOWN_TEAM_ID, KNOWN_SEASON_ID);

        // Then
        assertNotNull(stats);
        assertEquals(KNOWN_TEAM_ID, stats.getTeamId());
        assertEquals(KNOWN_SEASON_ID, stats.getSeasonId());
        assertEquals(KNOWN_TEAM_NAME, stats.getTeamName());
        assertEquals(KNOWN_LEAGUE_ID, stats.getLeagueId());
        assertEquals(KNOWN_LEAGUE_NAME, stats.getLeagueName());
        
        // Verify latest stats from live game
        assertEquals(125.0, stats.getPpg(), 0.1, "Should return latest PPG from live game");
        assertEquals(30.0, stats.getApg(), 0.1, "Should return latest APG from live game");
        assertEquals(50.0, stats.getRpg(), 0.1, "Should return latest RPG from live game");
        assertEquals(10.0, stats.getSpg(), 0.1, "Should return latest SPG from live game");
        assertEquals(8.0, stats.getBpg(), 0.1, "Should return latest BPG from live game");
        assertEquals(11.0, stats.getTopg(), 0.1, "Should return latest TOPG from live game");
        assertEquals(245.0, stats.getMpg(), 0.1, "Should return latest MPG from live game");
        assertEquals(2, stats.getGames(), "Should count as one game");
        
        assertNotNull(stats.getTime(), "Time should be set");
        assertNotNull(stats.getLastUpdated(), "Last updated should be set");
        log.info("Successfully verified latest team stats from live game");
    }

    @Test
    void getTeamStats_WhenTeamNotFound_ShouldReturnNull() {
        log.info("Testing getTeamStats for non-existent team");
        // When
        String nonExistentTeamId = "00000000-0000-0000-0000-999999999999";
        String nonExistentSeasonId = "00000000-0000-0000-0000-999999999998";
        TeamStats stats = teamsRepository.getTeamStats(nonExistentTeamId, nonExistentSeasonId);

        // Then
        assertNull(stats);
        log.info("Successfully verified null response for non-existent team");
    }
} 