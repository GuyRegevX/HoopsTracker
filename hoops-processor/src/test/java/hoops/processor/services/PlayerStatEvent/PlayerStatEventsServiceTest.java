package hoops.processor.services.PlayerStatEvent;

import hoops.processor.models.entities.PlayerStatEvents;
import hoops.processor.repositories.PlayerStatEvent.PlayerStatEventsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerStatEventsServiceTest {

    @Mock
    private PlayerStatEventsRepository playerStatEventsRepository;

    @InjectMocks
    private PlayerStatEventsServiceImpl playerStatEventsService;

    @Captor
    private ArgumentCaptor<PlayerStatEvents> playerStatEventsCaptor;

    private PlayerStatEvents testEvent;

    @BeforeEach
    void setUp() {
        // Setup test data
        testEvent = new PlayerStatEvents();
        testEvent.setId(1L);
        testEvent.setPlayerId(2L);
        testEvent.setGameId(3L);
        testEvent.setTeamId(4L);
        testEvent.setSeasonId(5L);
        testEvent.setEventType("points_scored");
        testEvent.setValue(2);
        testEvent.setEventTime(Instant.now());
        testEvent.setProcessedAt(Instant.now());
        testEvent.setPlayerName("John Doe");
    }

    @Test
    void save_Success() {
        // Arrange
        when(playerStatEventsRepository.save(any())).thenReturn(testEvent);

        // Act
        PlayerStatEvents result = playerStatEventsService.save(testEvent);

        // Assert
        assertNotNull(result);
        assertEquals(testEvent.getId(), result.getId());
        assertEquals(testEvent.getPlayerId(), result.getPlayerId());
        assertEquals(testEvent.getGameId(), result.getGameId());
        assertEquals(testEvent.getTeamId(), result.getTeamId());
        assertEquals(testEvent.getSeasonId(), result.getSeasonId());
        assertEquals(testEvent.getEventType(), result.getEventType());
        assertEquals(testEvent.getValue(), result.getValue());
        assertEquals(testEvent.getEventTime(), result.getEventTime());
        assertEquals(testEvent.getProcessedAt(), result.getProcessedAt());
        assertEquals(testEvent.getPlayerName(), result.getPlayerName());

        verify(playerStatEventsRepository).save(playerStatEventsCaptor.capture());
        PlayerStatEvents capturedEvent = playerStatEventsCaptor.getValue();
        assertEquals(testEvent, capturedEvent);
    }

    @Test
    void save_RepositoryError() {
        // Arrange
        when(playerStatEventsRepository.save(any())).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> playerStatEventsService.save(testEvent));
        assertEquals("Database error", exception.getMessage());
        verify(playerStatEventsRepository).save(testEvent);
    }
} 