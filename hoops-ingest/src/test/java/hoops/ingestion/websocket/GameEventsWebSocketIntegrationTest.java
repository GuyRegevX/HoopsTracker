package hoops.ingestion.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import hoops.common.constants.StreamConstants;
import hoops.ingestion.constants.WebSocketConstants;
import hoops.ingestion.config.WebSocketConfig;
import hoops.ingestion.config.TestRedisConfig;
import hoops.ingestion.config.JacksonConfig;
import hoops.ingestion.config.WebSocketTestConfig;
import hoops.ingestion.services.GameEventService;
import hoops.ingestion.services.GameEventServiceImpl;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.domain.Range;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.junit.jupiter.api.Disabled;
import org.testcontainers.junit.jupiter.Testcontainers;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.resps.StreamEntry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {
        WebSocketConfig.class,
        JacksonConfig.class,
        GameEventWebSocketHandler.class,
        WebSocketTestConfig.class
    }
)
@Testcontainers
@Import({TestRedisConfig.class})
@ActiveProfiles("test")
@Disabled("WebSocket integration test needs to be fixed")
class GameEventsWebSocketIntegrationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public ServletWebServerFactory servletWebServerFactory() {
            return new TomcatServletWebServerFactory();
        }

        @Bean
        public Validator validator() {
            return Validation.buildDefaultValidatorFactory().getValidator();
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JedisPool jedisPool;

    @MockBean
    private GameEventService gameEventService;

    private WebSocketSession webSocketSession;
    private List<String> receivedMessages;
    private WebSocketClient webSocketClient;

    @BeforeEach
    void setUp() throws Exception {
        // Clear Redis stream
        try (var jedis = jedisPool.getResource()) {
            jedis.del(StreamConstants.GAME_EVENTS_STREAM);
        }
        
        // Initialize WebSocket client
        receivedMessages = new ArrayList<>();
        webSocketClient = new StandardWebSocketClient();
        
        CompletableFuture<WebSocketSession> sessionFuture = new CompletableFuture<>();
        
        webSocketSession = webSocketClient.execute(
            new TextWebSocketHandler() {
                @Override
                public void handleTextMessage(WebSocketSession session, TextMessage message) {
                    receivedMessages.add(message.getPayload());
                }
                
                @Override
                public void afterConnectionEstablished(WebSocketSession session) {
                    sessionFuture.complete(session);
                }
            },
            uri()
        ).get(5, TimeUnit.SECONDS);
    }

    @Test
    void testValidMessageFlow() throws Exception {
        // Send valid message
        String validMessage = """
            {
                "gameId": "2024030100",
                "teamId": "BOS",
                "playerId": "jt0",
                "playerName": "Jayson Tatum",
                "event": "points",
                "value": 3,
                "timestamp": "%s"
            }
            """.formatted(Instant.now().toString());

        webSocketSession.sendMessage(new TextMessage(validMessage));

        // Wait for processing
        Thread.sleep(1000);

        // Check Redis stream
        try (var jedis = jedisPool.getResource()) {
            List<StreamEntry> streamEntries = jedis.xrange(StreamConstants.GAME_EVENTS_STREAM, (StreamEntryID)null, (StreamEntryID)null, 100);
            assertFalse(streamEntries.isEmpty(), "Stream should contain the valid message");
            Map<String, String> fields = streamEntries.get(0).getFields();
            assertTrue(fields.get("playerName").equals("Jayson Tatum"), 
                "Stream entry should contain player name");
        }
    }

    @Test
    void testInvalidMessageMissingField() throws Exception {
        // Send message with missing playerId
        String invalidMessage = """
            {
                "gameId": "2024030100",
                "teamId": "BOS",
                "playerName": "Jayson Tatum",
                "event": "points",
                "value": 3,
                "timestamp": "%s"
            }
            """.formatted(Instant.now().toString());

        webSocketSession.sendMessage(new TextMessage(invalidMessage));

        // Wait for processing
        Thread.sleep(1000);

        // Check Redis stream - should be empty
        try (var jedis = jedisPool.getResource()) {
            List<StreamEntry> streamEntries = jedis.xrange(StreamConstants.GAME_EVENTS_STREAM, (StreamEntryID)null, (StreamEntryID)null, 100);
            assertTrue(streamEntries.isEmpty(), "Stream should not contain invalid message");
        }
    }

    @Test
    void testInvalidMessageValidationError() throws Exception {
        // Send message with invalid points value (4 is not valid in basketball)
        String invalidMessage = """
            {
                "gameId": "2024030100",
                "teamId": "BOS",
                "playerId": "jt0",
                "playerName": "Jayson Tatum",
                "event": "points",
                "value": 4,
                "timestamp": "%s"
            }
            """.formatted(Instant.now().toString());

        webSocketSession.sendMessage(new TextMessage(invalidMessage));

        // Wait for processing
        Thread.sleep(1000);

        // Check Redis stream - should be empty
        try (var jedis = jedisPool.getResource()) {
            List<StreamEntry> streamEntries = jedis.xrange(StreamConstants.GAME_EVENTS_STREAM, (StreamEntryID)null, (StreamEntryID)null, 100);
            assertTrue(streamEntries.isEmpty(), "Stream should not contain message with validation error");
        }
    }

    private String uri() {
        return String.format("ws://localhost:%d%s", port, WebSocketConstants.GAME_EVENTS_ENDPOINT);
    }
} 