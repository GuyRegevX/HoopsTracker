package hoops.ingestion.producers.gameEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import hoops.common.constants.StreamConstants;
import hoops.common.models.events.AssistsEvent;
import hoops.common.models.events.GameEvent;
import hoops.common.models.events.PointsEvent;
import hoops.common.models.events.ReboundsEvent;
import hoops.ingestion.config.TestRedisConfig;
import hoops.ingestion.services.producers.GameEventProducerImpl;
import io.lettuce.core.*;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {
        GameEventProducerImpl.class,
        TestRedisConfig.class,
        ObjectMapper.class
})
@ActiveProfiles("test")
@Testcontainers
class GameEventProducerImplTest {

    @Autowired
    private GameEventProducerImpl gameEventProducer;

    @Autowired
    private RedisClient redisClient;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TEST_GROUP = "test-group";
    private static final String TEST_CONSUMER = "test-consumer";
    
    private RedisCommands<String, String> commands;

    @BeforeEach
    void setUp() {
        commands = redisClient.connect().sync();
        
        // Delete all messages from the stream, keeping 0 elements
        try {
            
            // Delete any consumer groups
            try {
                List<Object> groups = commands.xinfoGroups(StreamConstants.GAME_EVENTS_STREAM);
                for (Object groupObj : groups) {
                    // Group info is returned as a Map-like structure
                    @SuppressWarnings("unchecked")
                    Map<String, Object> group = (Map<String, Object>) groupObj;
                    String groupName = (String) group.get("name");
                    commands.xgroupDestroy(StreamConstants.GAME_EVENTS_STREAM, groupName);
                }
            } catch (Exception ignored) {
                // Stream might not exist yet, which is fine
            }
        } catch (Exception ignored) {
            // Stream might not exist yet, which is fine
        } finally {
            // Close the connection after setup
            if (commands != null) {
                commands.getStatefulConnection().close();
            }
        }
    }

    @AfterEach
    void tearDown() {
        // Close any remaining open connections
        if (commands != null) {
            try {
                commands.getStatefulConnection().close();
            } catch (Exception ignored) {
                // Connection might already be closed
            }
        }
    }

    @Test
    void publishAndConsumePointsEvent() throws Exception {
        // Arrange
        PointsEvent pointsEvent = new PointsEvent();
        pointsEvent.setGameId("2024031501");  // Format: YYYYMMDDGGG
        pointsEvent.setTeamId("BOS");
        pointsEvent.setPlayerId("player123");
        pointsEvent.setValue(3.0);  // 3-pointer
        pointsEvent.setVersion(1L);

        initConsumerGroup();

        // Act
        gameEventProducer.publishEvent(pointsEvent);

        // Assert
        commands = redisClient.connect().sync();
        try {
            // Read messages using consumer group
            List<StreamMessage<String, String>> messages = commands.xreadgroup(
                    Consumer.from(TEST_GROUP, TEST_CONSUMER),
                    XReadArgs.Builder.count(1).block(1000),
                    XReadArgs.StreamOffset.lastConsumed(StreamConstants.GAME_EVENTS_STREAM)
            );

            assertNotNull(messages);
            assertFalse(messages.isEmpty());
            assertEquals(1, messages.size());

            StreamMessage<String, String> message = messages.get(0);
            Map<String, String> fields = message.getBody();
            String eventJson = fields.get("data");
            GameEvent consumedEvent = objectMapper.readValue(eventJson, GameEvent.class);

            assertTrue(consumedEvent instanceof PointsEvent);
            PointsEvent consumedPointsEvent = (PointsEvent) consumedEvent;

            assertEquals("2024031501", consumedPointsEvent.getGameId());
            assertEquals("BOS", consumedPointsEvent.getTeamId());
            assertEquals("player123", consumedPointsEvent.getPlayerId());
            assertEquals(3.0, consumedPointsEvent.getValue());
            assertEquals(1L, consumedPointsEvent.getVersion());

            // Acknowledge the message
            commands.xack(StreamConstants.GAME_EVENTS_STREAM, TEST_GROUP, message.getId());
        } finally {
            commands.getStatefulConnection().close();
        }
    }

    @Test
    void publishMultipleEventTypes() throws Exception {
        // Arrange
        PointsEvent pointsEvent = createPointsEvent();
        ReboundsEvent reboundsEvent = createReboundsEvent();
        AssistsEvent assistsEvent = createAssistsEvent();

        initConsumerGroup();

        // Act
        gameEventProducer.publishEvent(pointsEvent);
        gameEventProducer.publishEvent(reboundsEvent);
        gameEventProducer.publishEvent(assistsEvent);

        // Assert
        commands = redisClient.connect().sync();
        try {
            // Read messages using consumer group
            List<StreamMessage<String, String>> messages = commands.xreadgroup(
                    Consumer.from(TEST_GROUP, TEST_CONSUMER),
                    XReadArgs.Builder.count(3),
                    XReadArgs.StreamOffset.lastConsumed(StreamConstants.GAME_EVENTS_STREAM)
            );

            assertNotNull(messages);
            assertEquals(3, messages.size());

            // Verify events in order
            verifyEvent(messages.get(0), PointsEvent.class, 2.0);
            verifyEvent(messages.get(1), ReboundsEvent.class, 1.0);
            verifyEvent(messages.get(2), AssistsEvent.class, 1.0);

            // Acknowledge all messages
            for (StreamMessage<String, String> message : messages) {
                commands.xack(StreamConstants.GAME_EVENTS_STREAM, TEST_GROUP, message.getId());
            }
        } finally {
            commands.getStatefulConnection().close();
        }
    }

    private void initConsumerGroup() {
        commands = redisClient.connect().sync();
        try {
            gameEventProducer.initStream();
            
            // Create consumer group - use $ to start consuming from new messages
            try {
                commands.xgroupCreate(
                        XReadArgs.StreamOffset.from(StreamConstants.GAME_EVENTS_STREAM, "$"),
                        TEST_GROUP,
                        XGroupCreateArgs.Builder.mkstream()
                );
            } catch (Exception e) {
                // Group might already exist, which is fine for the test
                if (!e.getMessage().contains("BUSYGROUP")) {
                    throw e;
                }
            }
        } finally {
            commands.getStatefulConnection().close();
        }
    }

    private PointsEvent createPointsEvent() {
        PointsEvent event = new PointsEvent();
        event.setGameId("2024031501");
        event.setTeamId("BOS");
        event.setPlayerId("player123");
        event.setValue(2.0);
        event.setVersion(1L);
        return event;
    }

    private ReboundsEvent createReboundsEvent() {
        ReboundsEvent event = new ReboundsEvent();
        event.setGameId("2024031501");
        event.setTeamId("BOS");
        event.setPlayerId("player123");
        event.setValue(1.0);
        event.setVersion(1L);
        return event;
    }

    private AssistsEvent createAssistsEvent() {
        AssistsEvent event = new AssistsEvent();
        event.setGameId("2024031501");
        event.setTeamId("BOS");
        event.setPlayerId("player123");
        event.setValue(1.0);
        event.setVersion(1L);
        return event;
    }

    private void verifyEvent(StreamMessage<String, String> message, Class<? extends GameEvent> expectedType, Double expectedValue)
            throws Exception {
        Map<String, String> fields = message.getBody();
        GameEvent consumedEvent = objectMapper.readValue(
                fields.get("data"),
                GameEvent.class
        );
        assertTrue(expectedType.isInstance(consumedEvent));
        assertEquals(expectedValue, consumedEvent.getValue());
    }
}