package hoops.processor.consumers;

import hoops.common.constants.StreamConstants;
import hoops.common.models.events.GameEvent;
import hoops.common.models.events.PointsEvent;
import hoops.processor.config.TestRedisConfig;
import hoops.processor.infrastructure.redis.RedisStreamManager;
import hoops.processor.processors.GameEvent.GameEventProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisClient;
import io.lettuce.core.XGroupCreateArgs;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {
        GameEventStreamConsumer.class,
        ObjectMapper.class,
        RedisStreamManager.class,
})
@Import(TestRedisConfig.class)
@ActiveProfiles("test")
class GameEventStreamConsumerTest {

    @Autowired
    private GameEventStreamConsumer listener;

    @Autowired
    private RedisClient redisClient;

    @MockBean
    private GameEventProcessor gameEventProcessor;

    @Autowired
    private ObjectMapper objectMapper;

    private RedisCommands<String, String> commands;

    @BeforeEach
    void setUp() {
        commands = redisClient.connect().sync();
        commands.flushall();  // This removes all data

        try {
            
            // Delete the consumer group if it exists
            try {
                commands.xgroupDestroy(StreamConstants.GAME_EVENTS_STREAM, "game-events-processor");
            } catch (Exception e) {
                // Ignore - group might not exist yet
            }

            // Create stream with initial message
            PointsEvent event = new PointsEvent();
            event.setVersion(2024030100L);
            event.setGameId("2024030100");
            event.setTeamId("BOS");
            event.setPlayerId("jt0");
            event.setValue(2.0);
            event.setEvent("point");

            String eventJson = objectMapper.writeValueAsString(event);

            commands.xadd(
                    StreamConstants.GAME_EVENTS_STREAM,
                    Map.of("data", eventJson)
            );

            // Now create the consumer group
            commands.xgroupCreate(
                    XReadArgs.StreamOffset.from(StreamConstants.GAME_EVENTS_STREAM, "$"),
                    "game-events-processor",
                    XGroupCreateArgs.Builder.mkstream()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up test", e);
        }
    }

    @Test
    void testProcessGameEvents() throws Exception {
        // Arrange
        GameEvent event = new PointsEvent();
        event.setGameId("game1");
        event.setPlayerId("player1");
        event.setValue(2.0);

        String eventJson = objectMapper.writeValueAsString(event);

        // Add event to stream
        commands.xadd(
                StreamConstants.GAME_EVENTS_STREAM,
                Map.of("data", eventJson)
        );

        // Act
        listener.processGameEvents();

        // Allow time for processing
        TimeUnit.SECONDS.sleep(1);

        // Assert
        verify(gameEventProcessor, times(1)).processEvent(any(GameEvent.class));
    }
}