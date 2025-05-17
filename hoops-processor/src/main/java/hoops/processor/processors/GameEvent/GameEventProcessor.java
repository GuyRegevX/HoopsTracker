package hoops.processor.processors.GameEvent;

import hoops.common.models.events.GameEvent;

public interface GameEventProcessor {
    void processEvent(GameEvent event);
}