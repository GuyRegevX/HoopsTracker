package hoops.ingestion.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import hoops.common.models.events.MinutesPlayedEvent;
import hoops.common.models.events.PointsEvent;
import hoops.ingestion.services.producers.GameEventProducer;
import hoops.ingestion.config.JacksonConfig;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = JacksonConfig.class)
@ExtendWith(MockitoExtension.class)
class GameEventWebSocketHandlerTest {
    
    @Mock
    private GameEventProducer gameEventProducer;
    
    @Mock
    private WebSocketSession webSocketSession;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private GameEventWebSocketHandler handler;
    
    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        handler = new GameEventWebSocketHandler(objectMapper, gameEventProducer, validator);
    }
    
    @Test
    void testValidMessageProcessing() throws Exception {
        // Create a valid game event message
        String json = """
            {
                "gameId": "2024030100",
                "teamId": "BOS",
                "playerId": "jt0",
                "event": "point",
                "value": 3,
                "version": 1
            }
            """.formatted(Instant.now().toString());
        
        TextMessage message = new TextMessage(json);
        
        // Configure mock behavior for a valid event
        doNothing().when(gameEventProducer).publishEvent(any(PointsEvent.class));
        
        // Handle the message
        handler.handleTextMessage(webSocketSession, message);
        
        // Verify that the service was called once
        verify(gameEventProducer, times(1)).publishEvent(any(PointsEvent.class));
    }
    
    @Test
    void testInvalidMessageProcessing() throws Exception {
        // Create an invalid game event message (missing required field)
        String json = """
            {
                "gameId": "2024030100",
                "teamId": "BOS",
                "playerName": "Jayson Tatum",
                "event": "point",
                "value": 4
                "version": 1
            }
            """.formatted(Instant.now().toString());
        
        TextMessage message = new TextMessage(json);
        
        // Handle the message
        handler.handleTextMessage(webSocketSession, message);
        
        // Verify that the service was not called
        verify(gameEventProducer, never()).publishEvent(any());
    }
    
    @Test
    void testMalformedJsonProcessing() throws Exception {
        // Create malformed JSON
        String json = "{ this is not valid json }";
        TextMessage message = new TextMessage(json);
        
        // Handle the message
        handler.handleTextMessage(webSocketSession, message);
        
        // Verify that the service was not called
        verify(gameEventProducer, never()).publishEvent(any());
    }
    
    @Test
    void testInvalidEventTypeProcessing() throws Exception {
        // Create message with invalid event type
        String json = """
            {
                "gameId": "2024030100",
                "teamId": "BOS",
                "playerId": "jt0",
                "playerName": "Jayson Tatum",
                "event": "invalid_event",
                "value": 3,
                "timestamp": "%s"
            }
            """.formatted(Instant.now().toString());
        
        TextMessage message = new TextMessage(json);
        
        // Handle the message
        handler.handleTextMessage(webSocketSession, message);
        
        // Verify that the service was not called
        verify(gameEventProducer, never()).publishEvent(any());
    }
    
    @Test
    void testInvalidValueProcessing() throws Exception {
        // Create message with invalid value for points (4 is not valid in basketball)
        String json = """
            {
                "gameId": "2024030100",
                "teamId": "BOS",
                "playerId": "jt0",
                "playerName": "Jayson Tatum",
                "event": "point",
                "value": 4,
                "version: 3,
                "timestamp": "%s"
            }
            """.formatted(Instant.now().toString());
        
        TextMessage message = new TextMessage(json);
        
        // Handle the message
        handler.handleTextMessage(webSocketSession, message);
        
        // Verify that the service was not called
        verify(gameEventProducer, never()).publishEvent(any());
    }

    @Test
    void testMinutesPlayedMessageProcessing() throws Exception {
        String json = """
        {
            "gameId": "2024030100",
            "teamId": "BOS",
            "playerId": "jt0",
            "event": "minutes_played",
            "value": 0.3,
            "version": 1
        }
        """;

        TextMessage message = new TextMessage(json);

        // Handle the message
        handler.handleTextMessage(webSocketSession, message);

        // Verify that the service was called once
        verify(gameEventProducer, times(1)).publishEvent(any(MinutesPlayedEvent.class));
    }

    @Test
    void testInvalidMinutesPlayedValueProcessing() throws Exception {
        // Test with invalid minutes (over 48 minutes)
        String json = """
        {
            "gameId": "2024030100",
            "teamId": "BOS",
            "playerId": "jt0",
            "event": "minutes_played",
            "value": 48.1,
            "version": 1
        }
        """;

        TextMessage message = new TextMessage(json);

        // Handle the message
        handler.handleTextMessage(webSocketSession, message);

        // Verify that the service was not called due to invalid value
        verify(gameEventProducer, never()).publishEvent(any());
    }
} 