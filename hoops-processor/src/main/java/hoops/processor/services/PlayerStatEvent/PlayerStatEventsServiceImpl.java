package hoops.processor.services.PlayerStatEvent;

import hoops.processor.models.entities.PlayerStatEvents;
import hoops.processor.repositories.PlayerStatEvent.PlayerStatEventsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerStatEventsServiceImpl implements PlayerStatEventsService {
    private final PlayerStatEventsRepository playerStatEventsRepository;

    @Override
    public PlayerStatEvents save(PlayerStatEvents event) {
        try {
            return playerStatEventsRepository.save(event);
        } catch (Exception e) {
            log.error("Error saving player stat event: {}", event, e);
            throw e;
        }
    }
} 