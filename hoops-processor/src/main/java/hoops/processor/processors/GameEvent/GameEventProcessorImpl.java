package hoops.processor.processors.GameEvent;

import hoops.common.enums.StatType;
import hoops.common.models.events.GameEvent;
import hoops.processor.models.entities.PlayerStatEvent;
import hoops.processor.models.entities.Seasons;
import hoops.processor.services.playerStatEvents.PlayerStatEventsService;
import hoops.processor.services.seasons.SeasonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameEventProcessorImpl implements GameEventProcessor {
    private final SeasonService seasonService;
    private final PlayerStatEventsService playerStatEventsService;

    @Override
    public void processEvent(GameEvent event) {
        try {
            Seasons currentSeason = seasonService.getCurrentSeason()
                .orElseThrow(() -> new RuntimeException("No active season found"));

            var playerStatEvent = PlayerStatEvent.builder()
                .version(event.getVersion())
                .playerId(event.getPlayerId())
                .teamId(event.getTeamId())
                .gameId(event.getGameId())
                .seasonId(currentSeason.getId())
                .statType(StatType.fromString(event.getEvent()))
                .statValue(event.getValue())
                .version(event.getVersion())
                .build();
            playerStatEventsService.save(playerStatEvent);
        } catch (Exception e) {
            log.error("Error processing game event: {}", event, e);
            throw new RuntimeException("Failed to process game event", e);
        }
    }
} 