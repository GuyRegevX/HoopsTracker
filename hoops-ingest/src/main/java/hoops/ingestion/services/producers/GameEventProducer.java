package hoops.ingestion.services.producers;

import hoops.common.models.events.GameEvent;

public interface GameEventProducer {
    /**
     * Process a game event and publish it to Redis stream
     * @param event The game event to process
     */
    void publishEvent(GameEvent event);
} 