package hoops.api.controllers;

import hoops.api.models.dtos.players.PlayerMetaDTO;
import hoops.api.models.dtos.players.PlayerStatsDTO;
import hoops.api.services.players.PlayerService;

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

@WebMvcTest(PlayersController.class)
class PlayersControllerTest {

    private static final Logger log = LoggerFactory.getLogger(PlayersControllerTest.class);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlayerService playerService;

    private PlayerMetaDTO testPlayerMetaDTO;
    private PlayerStatsDTO testPlayerStatsDTO;
    private static final String TEST_PLAYER_ID = "00000000-0000-0000-0000-000000000301";
    private static final String TEST_SEASON_ID = "00000000-0000-0000-0000-000000000101";

    @BeforeEach
    void setUp() {
        // Setup test PlayerMetaDTO
        testPlayerMetaDTO = new PlayerMetaDTO();
        testPlayerMetaDTO.setPlayerId(TEST_PLAYER_ID);
        testPlayerMetaDTO.setName("LeBron James");
        testPlayerMetaDTO.setTeamId("00000000-0000-0000-0000-000000000201");
        testPlayerMetaDTO.setTeamName("Los Angeles Lakers");
        testPlayerMetaDTO.setJerseyNumber(23);
        testPlayerMetaDTO.setPosition("Forward");
        testPlayerMetaDTO.setLastUpdated(OffsetDateTime.now());

        // Setup test PlayerStatsDTO
        testPlayerStatsDTO = new PlayerStatsDTO();
        testPlayerStatsDTO.setPlayerId(TEST_PLAYER_ID);
        testPlayerStatsDTO.setGames(2);
        testPlayerStatsDTO.setPpg(28.5);  // Average of (27 + 30) / 2
        testPlayerStatsDTO.setApg(8.5);   // Average of (8 + 9) / 2
        testPlayerStatsDTO.setRpg(7.5);   // Average of (7 + 8) / 2
        testPlayerStatsDTO.setSpg(1.5);   // Average of (1 + 2) / 2
        testPlayerStatsDTO.setBpg(1.0);   // Average of (1 + 1) / 2
        testPlayerStatsDTO.setTopg(3.5);  // Average of (3 + 4) / 2
        testPlayerStatsDTO.setMpg(35.5);  // Average of (35 + 36) / 2
    }

    @Test
    void getAllPlayers_ShouldReturnPlayersList() throws Exception {
        // Given
        List<PlayerMetaDTO> players = Arrays.asList(testPlayerMetaDTO);
        when(playerService.getAllPlayers()).thenReturn(players);

        // When/Then
        mockMvc.perform(get("/api/v1/players")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].playerId").value(TEST_PLAYER_ID))
                .andExpect(jsonPath("$[0].name").value("LeBron James"))
                .andExpect(jsonPath("$[0].teamName").value("Los Angeles Lakers"))
                .andExpect(jsonPath("$[0].position").value("Forward"));
    }

    @Test
    void getPlayerStats_ShouldReturnStats() throws Exception {
        // Given
        when(playerService.getPlayerStats(eq(TEST_PLAYER_ID), eq(TEST_SEASON_ID))).thenReturn(testPlayerStatsDTO);

        // When/Then
        mockMvc.perform(get("/api/v1/players/{playerId}/stats", TEST_PLAYER_ID)
                .param("seasonId", TEST_SEASON_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.playerId").value(TEST_PLAYER_ID))
                .andExpect(jsonPath("$.games").value(2))
                .andExpect(jsonPath("$.ppg").value(28.5))
                .andExpect(jsonPath("$.apg").value(8.5))
                .andExpect(jsonPath("$.rpg").value(7.5))
                .andExpect(jsonPath("$.spg").value(1.5))
                .andExpect(jsonPath("$.bpg").value(1.0))
                .andExpect(jsonPath("$.topg").value(3.5))
                .andExpect(jsonPath("$.mpg").value(35.5));
    }

    @Test
    void getPlayerStats_WhenPlayerNotFound_ShouldReturn404() throws Exception {
        // Given
        when(playerService.getPlayerStats(anyString(), anyString())).thenReturn(null);

        // When/Then
        mockMvc.perform(get("/api/v1/players/{playerId}/stats", TEST_PLAYER_ID)
                .param("seasonId", TEST_SEASON_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPlayerStats_WithMissingSeasonId_ShouldReturn400() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/v1/players/{playerId}/stats", TEST_PLAYER_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
} 