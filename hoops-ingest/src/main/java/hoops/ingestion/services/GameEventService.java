package hoops.ingestion.services;

import hoops.ingestion.models.events.GameEvent;

public interface GameEventService {
    /**
     * Process a game event and publish it to Redis stream
     * @param event The game event to process
     */
    void processGameEvent(GameEvent event);
} 