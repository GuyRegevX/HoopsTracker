package hoops.processor.repositories.PlayerStatEvents;

import hoops.processor.models.entities.PlayerStatEvent;

public interface PlayerStatEventsRepository {
    void save(PlayerStatEvent playerStatEvent) ;
} 