package hoops.processor.services.playerStatEvents;

import hoops.processor.models.entities.PlayerStatEvent;

public interface PlayerStatEventsService {
    void save(PlayerStatEvent event);
}