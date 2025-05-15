package hoops.processor.processors;

import hoops.processor.models.GameEvent;
import hoops.processor.models.entities.PlayerStatEvents;
import hoops.processor.models.entities.Seasons;
import hoops.processor.repositories.PlayerStatEvent.PlayerStatEventsRepository;
import hoops.processor.services.Season.SeasonsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameEventProcessorImplTest {

    @Mock
    private SeasonsService seasonsService;

    @Mock
    private PlayerStatEventsRepository playerStatEventsRepository;

    @InjectMocks
    private GameEventProcessorImpl processor;

    @Captor
    private ArgumentCaptor<PlayerStatEvents> playerStatEventsCaptor;

    private GameEvent testEvent;
    private Seasons activeSeason;

    @BeforeEach
    void setUp() {
        // Setup test event
        testEvent = new GameEvent();
        testEvent.setId(1L);
        testEvent.setPlayerId(2L);
        testEvent.setGameId(3L);
        testEvent.setTeamId(4L);
        testEvent.setType("points_scored");
        testEvent.setValue(2);
        testEvent.setEventTime(Instant.now());
        testEvent.setPlayerName("John Doe");

        // Setup active season
        activeSeason = new Seasons();
        activeSeason.setId(5L);
        activeSeason.setName("2023-24");
        activeSeason.setIsActive(true);
    }

    @Test
    void processGameEvent_Success() {
        // Arrange
        when(seasonsService.getCurrentSeason()).thenReturn(Optional.of(activeSeason));
        doNothing().when(playerStatEventsRepository).save(any());

        // Act
        assertDoesNotThrow(() -> processor.processGameEvent(testEvent));

        // Assert
        verify(playerStatEventsRepository).save(playerStatEventsCaptor.capture());
        PlayerStatEvents savedEvent = playerStatEventsCaptor.getValue();
        
        assertEquals(testEvent.getPlayerId(), savedEvent.getPlayerId());
        assertEquals(testEvent.getGameId(), savedEvent.getGameId());
        assertEquals(testEvent.getTeamId(), savedEvent.getTeamId());
        assertEquals(activeSeason.getId(), savedEvent.getSeasonId());
        assertEquals(testEvent.getType(), savedEvent.getEventType());
        assertEquals(testEvent.getValue(), savedEvent.getValue());
        assertEquals(testEvent.getEventTime(), savedEvent.getEventTime());
        assertEquals(testEvent.getPlayerName(), savedEvent.getPlayerName());
        assertNotNull(savedEvent.getProcessedAt());
    }

    @Test
    void processGameEvent_NoActiveSeason_ThrowsException() {
        // Arrange
        when(seasonsService.getCurrentSeason()).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> processor.processGameEvent(testEvent));
        assertEquals("No active season found", exception.getMessage());
        verify(playerStatEventsRepository, never()).save(any());
    }

    @Test
    void processGameEvent_RepositoryError_ThrowsException() {
        // Arrange
        when(seasonsService.getCurrentSeason()).thenReturn(Optional.of(activeSeason));
        doThrow(new RuntimeException("Database error"))
            .when(playerStatEventsRepository).save(any());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> processor.processGameEvent(testEvent));
    }

    @Test
    void processGameEvent_NullEvent_ThrowsException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> processor.processGameEvent(null));
        verify(playerStatEventsRepository, never()).save(any());
    }
} 