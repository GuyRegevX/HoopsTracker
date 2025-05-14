package hoops.api.services.players;

import hoops.api.models.dtos.players.PlayerMetaDTO;
import hoops.api.models.dtos.players.PlayerStatsDTO;
import hoops.api.mappers.PlayerMapper;
import hoops.api.models.entities.players.Player;
import hoops.api.models.entities.players.PlayerStats;
import hoops.api.repositories.players.PlayersRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class PlayersServiceTest {
    @Mock
    private PlayersRepository playersRepository;
    
    @Mock
    private PlayerMapper playerMapper;
    
    private PlayersServiceImpl playersService;
    
    private Player testPlayer;
    private PlayerStats testPlayerStats;
    private PlayerMetaDTO testPlayerMetaDTO;
    private PlayerStatsDTO testPlayerStatsDTO;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        playersService = new PlayersServiceImpl(playersRepository, playerMapper);
        
        // Setup test data
        testPlayer = new Player();
        testPlayer.setPlayerId("00000000-0000-0000-0000-000000000301");
        testPlayer.setName("LeBron James");
        testPlayer.setTeamId("00000000-0000-0000-0000-000000000201");
        testPlayer.setTeamName("Los Angeles Lakers");
        testPlayer.setJerseyNumber(23);
        testPlayer.setPosition("Forward");
        testPlayer.setCreatedAt(OffsetDateTime.now());
        testPlayer.setLastUpdated(OffsetDateTime.now());
        
        testPlayerMetaDTO = new PlayerMetaDTO();
        testPlayerMetaDTO.setPlayerId(testPlayer.getPlayerId());
        testPlayerMetaDTO.setName(testPlayer.getName());
        testPlayerMetaDTO.setTeamId(testPlayer.getTeamId());
        testPlayerMetaDTO.setTeamName(testPlayer.getTeamName());
        testPlayerMetaDTO.setJerseyNumber(testPlayer.getJerseyNumber());
        testPlayerMetaDTO.setPosition(testPlayer.getPosition());
        testPlayerMetaDTO.setLastUpdated(testPlayer.getLastUpdated());
        
        testPlayerStats = new PlayerStats();
        testPlayerStats.setPlayerId(testPlayer.getPlayerId());
        testPlayerStats.setSeasonId("00000000-0000-0000-0000-000000000101");
        testPlayerStats.setGames(2);
        testPlayerStats.setPpg(28.5);
        testPlayerStats.setApg(8.5);
        testPlayerStats.setRpg(7.5);
        testPlayerStats.setSpg(1.5);
        testPlayerStats.setBpg(1.0);
        testPlayerStats.setTopg(3.5);
        testPlayerStats.setMpg(35.5);
        testPlayerStats.setLastUpdated(OffsetDateTime.now());
        
        testPlayerStatsDTO = new PlayerStatsDTO();
        testPlayerStatsDTO.setPlayerId(testPlayerStats.getPlayerId());
        testPlayerStatsDTO.setGames(testPlayerStats.getGames());
        testPlayerStatsDTO.setPpg(testPlayerStats.getPpg());
        testPlayerStatsDTO.setApg(testPlayerStats.getApg());
        testPlayerStatsDTO.setRpg(testPlayerStats.getRpg());
        testPlayerStatsDTO.setSpg(testPlayerStats.getSpg());
        testPlayerStatsDTO.setBpg(testPlayerStats.getBpg());
        testPlayerStatsDTO.setTopg(testPlayerStats.getTopg());
        testPlayerStatsDTO.setMpg(testPlayerStats.getMpg());
    }
    
    @Test
    void getAllPlayers_ShouldReturnPlayersList() {
        // Given
        List<Player> players = Arrays.asList(testPlayer);
        when(playersRepository.getAllPlayers()).thenReturn(players);
        when(playerMapper.toPlayerMetaDTO(testPlayer)).thenReturn(testPlayerMetaDTO);
        
        // When
        List<PlayerMetaDTO> result = playersService.getAllPlayers();
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testPlayerMetaDTO, result.get(0));
        verify(playersRepository).getAllPlayers();
        verify(playerMapper).toPlayerMetaDTO(testPlayer);
    }
    
    @Test
    void getPlayerStats_ShouldReturnStats() {
        // Given
        String playerId = testPlayerStats.getPlayerId();
        String seasonId = testPlayerStats.getSeasonId();
        when(playersRepository.getPlayerStats(playerId, seasonId)).thenReturn(testPlayerStats);
        when(playerMapper.toPlayerStatsDTO(testPlayerStats)).thenReturn(testPlayerStatsDTO);
        
        // When
        PlayerStatsDTO result = playersService.getPlayerStats(playerId, seasonId);
        
        // Then
        assertNotNull(result);
        assertEquals(testPlayerStatsDTO, result);
        verify(playersRepository).getPlayerStats(playerId, seasonId);
        verify(playerMapper).toPlayerStatsDTO(testPlayerStats);
    }
    
    @Test
    void getPlayerStats_WhenPlayerNotFound_ShouldReturnNull() {
        // Given
        String playerId = "non-existent";
        String seasonId = "any-season";
        when(playersRepository.getPlayerStats(playerId, seasonId)).thenReturn(null);
        
        // When
        PlayerStatsDTO result = playersService.getPlayerStats(playerId, seasonId);
        
        // Then
        assertNull(result);
        verify(playersRepository).getPlayerStats(playerId, seasonId);
        verify(playerMapper, never()).toPlayerStatsDTO(any());
    }
} 