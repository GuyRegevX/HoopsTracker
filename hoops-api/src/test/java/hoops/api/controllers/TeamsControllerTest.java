package hoops.api.controllers;

import hoops.api.models.dtos.teams.TeamMetaDTO;
import hoops.api.models.dtos.teams.TeamStatsDTO;
import hoops.api.services.teams.TeamsService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TeamsController.class)
class TeamsControllerTest {

    private static final Logger log = LoggerFactory.getLogger(TeamsControllerTest.class);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeamsService teamsService;

    private TeamMetaDTO testTeamMetaDTO;
    private TeamStatsDTO testTeamStatsDTO;
    private static final String TEST_TEAM_ID = "1";
    private static final String TEST_SEASON_ID = "1";

    @BeforeEach
    void setUp() {
        // Setup test TeamMetaDTO
        testTeamMetaDTO = new TeamMetaDTO();
        testTeamMetaDTO.setTeamId(TEST_TEAM_ID);
        testTeamMetaDTO.setName("Los Angeles Lakers");
        testTeamMetaDTO.setLeagueId("1");
        testTeamMetaDTO.setLeagueName("NBA");
        testTeamMetaDTO.setCountry("USA");
        testTeamMetaDTO.setLastUpdated(OffsetDateTime.now());

        // Setup test TeamStatsDTO
        testTeamStatsDTO = new TeamStatsDTO();
        testTeamStatsDTO.setTeamId(TEST_TEAM_ID);
        testTeamStatsDTO.setGames(2);  // Total of 2 games (1 completed + 1 live)
        testTeamStatsDTO.setPpg(27.5);  // Average of (30 + 25) / 2
        testTeamStatsDTO.setApg(9.0);   // Average of (10 + 8) / 2
        testTeamStatsDTO.setRpg(7.0);   // Average of (8 + 6) / 2
        testTeamStatsDTO.setSpg(1.5);   // Average of (2 + 1) / 2
        testTeamStatsDTO.setBpg(1.5);   // Average of (1 + 2) / 2
        testTeamStatsDTO.setTopg(2.5);  // Average of (3 + 2) / 2
        testTeamStatsDTO.setMpg(31.5);  // Average of (35 + 28) / 2
    }

    @Test
    void getAllTeams_ShouldReturnTeamsList() throws Exception {
        // Given
        List<TeamMetaDTO> teams = Arrays.asList(testTeamMetaDTO);
        when(teamsService.getAllTeams()).thenReturn(teams);

        // When/Then
        mockMvc.perform(get("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].teamId").value(TEST_TEAM_ID))
                .andExpect(jsonPath("$[0].name").value("Los Angeles Lakers"))
                .andExpect(jsonPath("$[0].leagueName").value("NBA"))
                .andExpect(jsonPath("$[0].country").value("USA"));
    }

    @Test
    void getTeamStats_ShouldReturnStats() throws Exception {
        // Given
        when(teamsService.getTeamStats(eq(TEST_TEAM_ID), eq(TEST_SEASON_ID))).thenReturn(testTeamStatsDTO);

        // When/Then
        mockMvc.perform(get("/api/v1/teams/{teamId}/stats", TEST_TEAM_ID)
                .param("seasonId", TEST_SEASON_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.teamId").value(TEST_TEAM_ID))
                .andExpect(jsonPath("$.games").value(2))
                .andExpect(jsonPath("$.ppg").value(27.5))
                .andExpect(jsonPath("$.apg").value(9.0))
                .andExpect(jsonPath("$.rpg").value(7.0))
                .andExpect(jsonPath("$.spg").value(1.5))
                .andExpect(jsonPath("$.bpg").value(1.5))
                .andExpect(jsonPath("$.topg").value(2.5))
                .andExpect(jsonPath("$.mpg").value(31.5));
    }

    @Test
    void getTeamStats_WhenTeamNotFound_ShouldReturn404() throws Exception {
        // Given
        when(teamsService.getTeamStats(anyString(), anyString())).thenReturn(null);

        // When/Then
        mockMvc.perform(get("/api/v1/teams/{teamId}/stats", TEST_TEAM_ID)
                .param("seasonId", TEST_SEASON_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTeamStats_WithMissingSeasonId_ShouldReturn400() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/v1/teams/{teamId}/stats", TEST_TEAM_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
} 