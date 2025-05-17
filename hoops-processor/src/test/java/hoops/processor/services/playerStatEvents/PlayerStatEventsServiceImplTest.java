package hoops.processor.services.playerStatEvents;

import hoops.common.enums.StatType;
import hoops.common.redis.RedisKeyUtil;
import hoops.processor.models.entities.PlayerStatEvent;
import hoops.processor.repositories.PlayerStatEvents.PlayerStatEventsRepository;
import io.lettuce.core.RedisClient;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerStatEventsServiceImplTest {

    @Mock
    private PlayerStatEventsRepository playerStatEventsRepository;
    
    @Mock
    private RedisClient redisClient;
    
    @Mock
    private StatefulRedisConnection<String, String> redisConnection;
    
    @Mock
    private RedisCommands<String, String> redisCommands;
    
    @InjectMocks
    private PlayerStatEventsServiceImpl playerStatEventsService;
    
    private PlayerStatEvent testEvent;
    private final String PLAYER_ID = "player123";
    private final String GAME_ID = "game456";
    private final String TEAM_ID = "team789";
    private final String SEASON_ID = "season2023";
    private String playerStatsKey;
    private String teamStatsKey;

    @BeforeEach
    void setUp() {
        // Set up test event
        testEvent = PlayerStatEvent.builder()
                .playerId(PLAYER_ID)
                .gameId(GAME_ID)
                .teamId(TEAM_ID)
                .seasonId(SEASON_ID)
                .statType(StatType.POINT)
                .statValue(2.0)
                .version(1L)
                .build();
                
        // Set up keys that will be used
        playerStatsKey = RedisKeyUtil.getPlayerStatsKey(PLAYER_ID, SEASON_ID);
        teamStatsKey = RedisKeyUtil.getTeamStatsKey(TEAM_ID, SEASON_ID);
    }

    @Test
    void save_shouldStoreInDatabaseAndInvalidateCache() {
        // Arrange
        // Set up Redis mocks for this test
        when(redisClient.connect()).thenReturn(redisConnection);
        when(redisConnection.sync()).thenReturn(redisCommands);
        when(redisCommands.getStatefulConnection()).thenReturn(redisConnection);
        
        when(redisCommands.multi()).thenReturn(null);
        // Create a TransactionResult mock - we only need it to return something non-null
        // The actual values aren't used by the implementation
        TransactionResult transactionResult = mock(TransactionResult.class);
        when(redisCommands.exec()).thenReturn(transactionResult);
        doNothing().when(playerStatEventsRepository).save(any(PlayerStatEvent.class));
        
        // Act
        playerStatEventsService.save(testEvent);
        
        // Assert
        // Verify repository was called to save the event
        verify(playerStatEventsRepository).save(testEvent);
        
        // Verify Redis operations
        verify(redisCommands).multi();
        verify(redisCommands).del(playerStatsKey);
        verify(redisCommands).del(teamStatsKey);
        verify(redisCommands).exec();
        verify(redisCommands).getStatefulConnection();
        verify(redisConnection).close();
    }
    
    @Test
    void save_shouldHandleDatabaseException() {
        // Arrange
        doThrow(new RuntimeException("Database error"))
            .when(playerStatEventsRepository).save(any(PlayerStatEvent.class));
        
        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            playerStatEventsService.save(testEvent);
        });
        
        // Verify exception and that Redis was never called
        assertTrue(exception.getMessage().contains("Failed to process player stat event"));
        verify(playerStatEventsRepository).save(testEvent);
        verify(redisClient, never()).connect();
    }
    
    @Test
    void save_shouldHandleRedisConnectionException() {
        // Arrange
        doNothing().when(playerStatEventsRepository).save(any(PlayerStatEvent.class));
        when(redisClient.connect()).thenThrow(new RuntimeException("Redis connection error"));
        
        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            playerStatEventsService.save(testEvent);
        });
        
        // Verify DB save was attempted before Redis error
        verify(playerStatEventsRepository).save(testEvent);
        verify(redisClient).connect();
        assertTrue(exception.getMessage().contains("Failed to process player stat event"));
    }
    
    @Test
    void save_shouldHandleRedisTransactionException() {
        // Arrange
        doNothing().when(playerStatEventsRepository).save(any(PlayerStatEvent.class));
        
        // Set up Redis mocks for this test
        when(redisClient.connect()).thenReturn(redisConnection);
        when(redisConnection.sync()).thenReturn(redisCommands);
        when(redisCommands.getStatefulConnection()).thenReturn(redisConnection);
        
        when(redisCommands.multi()).thenThrow(new RuntimeException("Redis transaction error"));
        
        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            playerStatEventsService.save(testEvent);
        });
        
        // Verify DB save was attempted and connection was closed despite error
        verify(playerStatEventsRepository).save(testEvent);
        verify(redisCommands).multi();
        verify(redisCommands).getStatefulConnection();
        verify(redisConnection).close();
        assertTrue(exception.getMessage().contains("Failed to process player stat event"));
    }
    
    @Test
    void save_shouldHandleRedisExecException() {
        // Arrange
        doNothing().when(playerStatEventsRepository).save(any(PlayerStatEvent.class));
        
        // Set up Redis mocks for this test
        when(redisClient.connect()).thenReturn(redisConnection);
        when(redisConnection.sync()).thenReturn(redisCommands);
        when(redisCommands.getStatefulConnection()).thenReturn(redisConnection);
        
        when(redisCommands.multi()).thenReturn(null);
        when(redisCommands.exec()).thenThrow(new RuntimeException("Redis exec error"));
        when(redisCommands.discard()).thenReturn("OK"); // Lettuce Redis commands typically return String status
        
        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            playerStatEventsService.save(testEvent);
        });
        
        // Verify transaction was discarded and connection closed
        verify(playerStatEventsRepository).save(testEvent);
        verify(redisCommands).multi();
        verify(redisCommands, times(2)).del(anyString()); // Two del calls are expected
        verify(redisCommands).exec();
        verify(redisCommands).discard();
        verify(redisCommands).getStatefulConnection();
        verify(redisConnection).close();
        assertTrue(exception.getMessage().contains("Failed to process player stat event"));
    }
}