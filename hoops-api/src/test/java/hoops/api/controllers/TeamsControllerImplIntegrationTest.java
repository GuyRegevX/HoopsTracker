package hoops.api.controllers;

import hoops.api.config.TestTimescaleDBConfig;
import hoops.api.models.dtos.teams.TeamMetaDTO;
import hoops.api.models.dtos.teams.TeamStatsDTO;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {TestTimescaleDBConfig.class}
)
@Testcontainers
@ActiveProfiles("test")
class TeamsControllerImplIntegrationTest {


    @Autowired
    private TestRestTemplate restTemplate;

    private final String KNOWN_TEAM_ID = "1";
    private final String KNOWN_SEASON_ID = "1";

    @Test
    void getAllTeams_ShouldReturnTeamsFromDatabase() {
        // When
        ResponseEntity<List<TeamMetaDTO>> response = restTemplate.exchange(
            "/api/v1/teams",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<TeamMetaDTO>>() {}
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<TeamMetaDTO> teams = response.getBody();
        assertNotNull(teams);
        assertFalse(teams.isEmpty());

        TeamMetaDTO team = teams.get(0);
        assertEquals("Los Angeles Lakers", team.getName());
        assertEquals(KNOWN_TEAM_ID, team.getTeamId());
        assertEquals("NBA", team.getLeagueName());
        assertEquals("USA", team.getCountry());    
    }

    @Test
    void getTeamStats_ShouldReturnLatestStats() {
        // When
        String url = String.format("/api/v1/teams/%s/stats?seasonId=%s", 
            KNOWN_TEAM_ID, KNOWN_SEASON_ID);
        ResponseEntity<TeamStatsDTO> response = restTemplate.getForEntity(url, TeamStatsDTO.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TeamStatsDTO stats = response.getBody();
        assertNotNull(stats);
        assertEquals(KNOWN_TEAM_ID, stats.getTeamId());
        assertEquals(2, stats.getGames());     // Total games (1 completed + 1 live)
        assertEquals(125.0, stats.getPpg());   // Average of (118 + 132) / 2
        assertEquals(30.0, stats.getApg());    // Average of (27 + 33) / 2
        assertEquals(50.0, stats.getRpg());    // Average of (46 + 54) / 2
        assertEquals(10.0, stats.getSpg());    // Average of (9 + 11) / 2
        assertEquals(8.0, stats.getBpg());     // Average of (6 + 10) / 2
        assertEquals(11.0, stats.getTopg());   // Average of (12 + 10) / 2
        assertEquals(245.0, stats.getMpg());   // Average of (243 + 247) / 2
    }

    @Test
    void getTeamStats_ShouldReturn404ForNonExistentTeam() {
        // When
        String nonExistentTeamId = UUID.randomUUID().toString();
        String url = String.format("/api/v1/teams/%s/stats?seasonId=%s", 
            nonExistentTeamId, KNOWN_SEASON_ID);
        ResponseEntity<TeamStatsDTO> response = restTemplate.getForEntity(url, TeamStatsDTO.class);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
} 