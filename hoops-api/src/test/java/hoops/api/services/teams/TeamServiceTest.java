package hoops.api.services.teams;

import hoops.api.models.dtos.teams.TeamMetaDTO;
import hoops.api.models.dtos.teams.TeamStatsDTO;
import hoops.api.mappers.TeamMapper;
import hoops.api.models.entities.teams.Team;
import hoops.api.models.entities.teams.TeamStats;
import hoops.api.repositories.teams.TeamsRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class TeamServiceTest {
    @Mock
    private TeamsRepository teamsRepository;
    
    @Mock
    private TeamMapper teamMapper;
    
    private TeamsServiceImpl teamsService;
    
    private Team testTeam;
    private TeamStats testTeamStats;
    private TeamMetaDTO testTeamMetaDTO;
    private TeamStatsDTO testTeamStatsDTO;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        teamsService = new TeamsServiceImpl(teamsRepository, teamMapper);
        
        // Setup test data
        testTeam = new Team();
        testTeam.setTeamId("00000000-0000-0000-0000-000000000201");
        testTeam.setName("Los Angeles Lakers");
        testTeam.setLeagueId("00000000-0000-0000-0000-000000000101");
        testTeam.setLeagueName("NBA");
        testTeam.setCountry("USA");
        testTeam.setCity("Los Angeles");
        testTeam.setDivision("Pacific");
        testTeam.setConference("Western");
        testTeam.setArena("Crypto.com Arena");
        testTeam.setFoundedYear(1947);
        testTeam.setCreatedAt(OffsetDateTime.now());
        testTeam.setLastUpdated(OffsetDateTime.now());
        
        testTeamMetaDTO = new TeamMetaDTO();
        testTeamMetaDTO.setTeamId(testTeam.getTeamId());
        testTeamMetaDTO.setName(testTeam.getName());
        testTeamMetaDTO.setLeagueId(testTeam.getLeagueId());
        testTeamMetaDTO.setLeagueName(testTeam.getLeagueName());
        testTeamMetaDTO.setCountry(testTeam.getCountry());
        testTeamMetaDTO.setLastUpdated(testTeam.getLastUpdated());
        
        testTeamStats = new TeamStats();
        testTeamStats.setTeamId(testTeam.getTeamId());
        testTeamStats.setSeasonId("00000000-0000-0000-0000-000000000101");
        testTeamStats.setGames(82);
        testTeamStats.setPpg(112.5);
        testTeamStats.setApg(25.3);
        testTeamStats.setRpg(44.8);
        testTeamStats.setSpg(7.5);
        testTeamStats.setBpg(5.2);
        testTeamStats.setTopg(14.1);
        testTeamStats.setMpg(240.0);
        testTeamStats.setLastUpdated(OffsetDateTime.now());
        
        testTeamStatsDTO = new TeamStatsDTO();
        testTeamStatsDTO.setTeamId(testTeamStats.getTeamId());
        testTeamStatsDTO.setGames(testTeamStats.getGames());
        testTeamStatsDTO.setPpg(testTeamStats.getPpg());
        testTeamStatsDTO.setApg(testTeamStats.getApg());
        testTeamStatsDTO.setRpg(testTeamStats.getRpg());
        testTeamStatsDTO.setSpg(testTeamStats.getSpg());
        testTeamStatsDTO.setBpg(testTeamStats.getBpg());
        testTeamStatsDTO.setTopg(testTeamStats.getTopg());
        testTeamStatsDTO.setMpg(testTeamStats.getMpg());
    }
    
    @Test
    void getAllTeams_ShouldReturnTeamsList() {
        // Given
        List<Team> teams = Arrays.asList(testTeam);
        when(teamsRepository.getAllTeams()).thenReturn(teams);
        when(teamMapper.toTeamMetaDTO(testTeam)).thenReturn(testTeamMetaDTO);
        
        // When
        List<TeamMetaDTO> result = teamsService.getAllTeams();
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testTeamMetaDTO, result.get(0));
        verify(teamsRepository).getAllTeams();
        verify(teamMapper).toTeamMetaDTO(testTeam);
    }
    
    @Test
    void getTeamStats_ShouldReturnStats() {
        // Given
        String teamId = testTeamStats.getTeamId();
        String seasonId = testTeamStats.getSeasonId();
        when(teamsRepository.getTeamStats(teamId, seasonId)).thenReturn(testTeamStats);
        when(teamMapper.toTeamStatsDTO(testTeamStats)).thenReturn(testTeamStatsDTO);
        
        // When
        TeamStatsDTO result = teamsService.getTeamStats(teamId, seasonId);
        
        // Then
        assertNotNull(result);
        assertEquals(testTeamStatsDTO, result);
        verify(teamsRepository).getTeamStats(teamId, seasonId);
        verify(teamMapper).toTeamStatsDTO(testTeamStats);
    }
    
    @Test
    void getTeamStats_WhenTeamNotFound_ShouldReturnNull() {
        // Given
        String teamId = "non-existent";
        String seasonId = "any-season";
        when(teamsRepository.getTeamStats(teamId, seasonId)).thenReturn(null);
        
        // When
        TeamStatsDTO result = teamsService.getTeamStats(teamId, seasonId);
        
        // Then
        assertNull(result);
        verify(teamsRepository).getTeamStats(teamId, seasonId);
        verify(teamMapper, never()).toTeamStatsDTO(any());
    }
} 