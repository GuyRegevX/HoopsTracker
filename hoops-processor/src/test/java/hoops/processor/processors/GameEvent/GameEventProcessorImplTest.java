package hoops.processor.processors.GameEvent;

import hoops.common.models.events.GameEvent;
import hoops.common.models.events.PointsEvent;
import hoops.processor.models.entities.PlayerStatEvent;
import hoops.processor.models.entities.Seasons;
import hoops.processor.repositories.PlayerStatEvents.PlayerStatEventsRepository;
import hoops.processor.services.playerStatEvents.PlayerStatEventsServiceImpl;
import hoops.processor.services.seasons.SeasonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameEventProcessorImplTest {

    @Mock
    private SeasonService seasonService;

    @Mock
    private PlayerStatEventsServiceImpl playerStatEventsService;

    @InjectMocks
    private GameEventProcessorImpl processor;

    @Captor
    private ArgumentCaptor<PlayerStatEvent> playerStatEventsCaptor;

    private GameEvent testEvent;
    private Seasons activeSeason;
    private final String PLAYER_ID = "1";
    private final String GAME_ID = "2";
    private final String TEAM_ID = "3";
    private final String SEASON_ID = "4";

    @BeforeEach
    void setUp() {
        // Setup test event
        testEvent = new PointsEvent();
        testEvent.setVersion(22L);
        testEvent.setPlayerId(PLAYER_ID);
        testEvent.setGameId(GAME_ID);
        testEvent.setTeamId(TEAM_ID);
        testEvent.setEvent("point");
        testEvent.setValue(2.0);

        // Setup active season
        activeSeason = Seasons.builder()
            .id(SEASON_ID)
            .name("2023-2024")
            .isActive(true)
            .build();
    }

    @Test
    void processEvent_Success() {
        when(seasonService.getCurrentSeason()).thenReturn(Optional.of(activeSeason));

        // Act
        processor.processEvent(testEvent);

        // Assert
        verify(seasonService).getCurrentSeason();
        verify(playerStatEventsService).save(playerStatEventsCaptor.capture());
        
        PlayerStatEvent capturedEvent = playerStatEventsCaptor.getValue();
        assertNotNull(capturedEvent);
        assertEquals(PLAYER_ID, capturedEvent.getPlayerId());
        assertEquals(TEAM_ID, capturedEvent.getTeamId());
        assertEquals(GAME_ID, capturedEvent.getGameId());
        assertEquals(SEASON_ID, capturedEvent.getSeasonId());
        assertEquals(testEvent.getEvent(), capturedEvent.getStatType().getValue());
        assertEquals(testEvent.getValue(), capturedEvent.getStatValue());
        assertEquals(testEvent.getVersion(), capturedEvent.getVersion());
    }

    @Test
    void processEvent_NoActiveSeason() {
        when(seasonService.getCurrentSeason()).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            processor.processEvent(testEvent);
        });

        assertEquals("Failed to process game event", exception.getMessage());
        verify(seasonService).getCurrentSeason();
        verify(playerStatEventsService, never()).save(any());
    }

    @Test
    void processEvent_ServiceError() {
        when(seasonService.getCurrentSeason()).thenThrow(new RuntimeException("Database error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            processor.processEvent(testEvent);
        });

        assertEquals("Failed to process game event", exception.getMessage());
        verify(seasonService).getCurrentSeason();
        verify(playerStatEventsService, never()).save(any());
    }
} 