package hoops.ingestion.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import hoops.ingestion.models.events.GameEvent;
import hoops.ingestion.services.GameEventService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * WebSocket handler for processing real-time game events.
 * This handler receives JSON messages containing game statistics,
 * validates them, and forwards them to the event processing service.
 */
@Component
public class GameEventWebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(GameEventWebSocketHandler.class);
    
    private final ObjectMapper objectMapper;      // For JSON serialization/deserialization
    private final GameEventService gameEventService;  // Service to process events
    private final Validator validator;            // For event validation
    
    /**
     * Constructor with required dependencies.
     * @param objectMapper For JSON parsing
     * @param gameEventService For event processing
     * @param validator For event validation
     */
    @Autowired
    public GameEventWebSocketHandler(ObjectMapper objectMapper, GameEventService gameEventService, Validator validator) {
        this.objectMapper = objectMapper;
        this.gameEventService = gameEventService;
        this.validator = validator;
    }
    
    /**
     * Handles incoming WebSocket messages.
     * Process flow:
     * 1. Parse JSON message to GameEvent object
     * 2. Validate the event data
     * 3. If valid, process the event
     * 4. If invalid, log the validation errors
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            logger.info("Received game event: {}", payload);
            
            // Parse JSON to GameEvent object
            GameEvent event = objectMapper.readValue(payload, GameEvent.class);
            
            // Validate all fields using Jakarta validation
            Set<ConstraintViolation<GameEvent>> violations = validator.validate(event);
            if (violations.isEmpty()) {
                // Process valid event
                gameEventService.processGameEvent(event);
            } else {
                // Log validation errors
                String errors = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));
                logger.error("Validation failed for game event: {}", errors);
            }
        } catch (Exception e) {
            logger.error("Error processing game event", e);
        }
    }
    
    /**
     * Handles WebSocket transport errors.
     * This includes connection issues, protocol errors, etc.
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        logger.error("WebSocket transport error", exception);
    }
} 