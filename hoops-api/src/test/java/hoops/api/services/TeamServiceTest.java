package hoops.api.services;

import hoops.api.models.dtos.TeamMetaDTO;
import hoops.api.models.dtos.TeamStatsDTO;
import hoops.api.mappers.TeamMapper;
import hoops.api.models.entities.Team;
import hoops.api.models.entities.TeamStats;
import hoops.api.repositories.TeamsRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamsRepository teamRepository;

    @Mock
    private TeamMapper teamMapper;

    @InjectMocks
    private TeamsServiceImpl teamService;

    private Team testTeam;
    private TeamStats testTeamStats;
    private TeamMetaDTO testTeamMetaDTO;
    private TeamStatsDTO testTeamStatsDTO;
    private static final String TEST_TEAM_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String TEST_SESSION_ID = "123e4567-e89b-12d3-a456-426614174001";

    @BeforeEach
    void setUp() {
        // Setup test team for metadata
        testTeam = new Team();
        testTeam.setTeamId(TEST_TEAM_ID);
        testTeam.setName("Los Angeles Lakers");
        testTeam.setLeagueId("nba");
        testTeam.setLeagueName("NBA");
        testTeam.setCountry("USA");
        testTeam.setCity("Los Angeles");
        testTeam.setDivision("Pacific");
        testTeam.setConference("Western");
        testTeam.setArena("Crypto.com Arena");
        testTeam.setFoundedYear(1947);
        testTeam.setLastUpdated(OffsetDateTime.now());

        // Setup test team stats
        testTeamStats = new TeamStats();
        testTeamStats.setTeamId(TEST_TEAM_ID);
        testTeamStats.setSeasonId(TEST_SESSION_ID);
        testTeamStats.setTeamName("Los Angeles Lakers");
        testTeamStats.setLeagueId("nba");
        testTeamStats.setLeagueName("NBA");
        testTeamStats.setGames(2);  // Total of 2 games (1 completed + 1 live)
        testTeamStats.setPpg(125.0);  // Average of (118 + 132) / 2
        testTeamStats.setApg(30.0);   // Average of (27 + 33) / 2
        testTeamStats.setRpg(50.0);   // Average of (46 + 54) / 2
        testTeamStats.setSpg(10.0);   // Average of (9 + 11) / 2
        testTeamStats.setBpg(8.0);    // Average of (6 + 10) / 2
        testTeamStats.setTopg(11.0);  // Average of (12 + 10) / 2
        testTeamStats.setMpg(245.0);  // Average of (243 + 247) / 2
        testTeamStats.setTime(OffsetDateTime.now());
        testTeamStats.setLastUpdated(OffsetDateTime.now());

        // Setup test TeamMetaDTO
        testTeamMetaDTO = new TeamMetaDTO();
        testTeamMetaDTO.setTeamId(TEST_TEAM_ID);
        testTeamMetaDTO.setName("Los Angeles Lakers");
        testTeamMetaDTO.setLeagueId("nba");
        testTeamMetaDTO.setLeagueName("NBA");
        testTeamMetaDTO.setCountry("USA");       
        testTeamMetaDTO.setLastUpdated(testTeam.getLastUpdated());

        // Setup test TeamStatsDTO
        testTeamStatsDTO = new TeamStatsDTO();
        testTeamStatsDTO.setTeamId(TEST_TEAM_ID);
        testTeamStatsDTO.setGames(82);
        testTeamStatsDTO.setPpg(118.5);
        testTeamStatsDTO.setApg(27.2);
        testTeamStatsDTO.setRpg(46.5);
        testTeamStatsDTO.setSpg(9.0);
        testTeamStatsDTO.setBpg(6.2);
        testTeamStatsDTO.setTopg(12.0);
        testTeamStatsDTO.setMpg(243.0);
    }

    @Test
    void getAllTeams_ShouldReturnMappedTeams() {
        // Given
        List<Team> teams = Arrays.asList(testTeam);
        when(teamRepository.getAllTeams()).thenReturn(teams);
        when(teamMapper.toTeamMetaDTO(testTeam)).thenReturn(testTeamMetaDTO);

        // When
        List<TeamMetaDTO> result = teamService.getAllTeams();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        TeamMetaDTO resultTeam = result.get(0);
        assertEquals(TEST_TEAM_ID, resultTeam.getTeamId());
        assertEquals("Los Angeles Lakers", resultTeam.getName());
        assertEquals("NBA", resultTeam.getLeagueName());
        assertEquals("USA", resultTeam.getCountry());
     
        verify(teamRepository).getAllTeams();
        verify(teamMapper).toTeamMetaDTO(testTeam);
    }

    @Test
    void getTeamStats_ShouldReturnMappedStats() {
        // Given
        when(teamRepository.getTeamStats(TEST_TEAM_ID, TEST_SESSION_ID)).thenReturn(testTeamStats);
        when(teamMapper.toTeamStatsDTO(testTeamStats)).thenReturn(testTeamStatsDTO);

        // When
        TeamStatsDTO result = teamService.getTeamStats(TEST_TEAM_ID, TEST_SESSION_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_TEAM_ID, result.getTeamId());
        assertEquals(82, result.getGames());
        assertEquals(118.5, result.getPpg());
        assertEquals(27.2, result.getApg());
        assertEquals(46.5, result.getRpg());
        assertEquals(9.0, result.getSpg());
        assertEquals(6.2, result.getBpg());
        assertEquals(12.0, result.getTopg());
        assertEquals(243.0, result.getMpg());

        verify(teamRepository).getTeamStats(TEST_TEAM_ID, TEST_SESSION_ID);
        verify(teamMapper).toTeamStatsDTO(testTeamStats);
    }

    @Test
    void getTeamStats_WhenTeamNotFound_ShouldReturnNull() {
        // Given
        when(teamRepository.getTeamStats(TEST_TEAM_ID, TEST_SESSION_ID)).thenReturn(null);

        // When
        TeamStatsDTO result = teamService.getTeamStats(TEST_TEAM_ID, TEST_SESSION_ID);

        // Then
        assertNull(result);
        verify(teamRepository).getTeamStats(TEST_TEAM_ID, TEST_SESSION_ID);
        verify(teamMapper, never()).toTeamStatsDTO(any());
    }
} 