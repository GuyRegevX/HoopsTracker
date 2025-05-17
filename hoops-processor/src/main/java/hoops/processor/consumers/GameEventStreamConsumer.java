package hoops.processor.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import hoops.common.constants.StreamConstants;
import hoops.common.models.events.GameEvent;
import hoops.processor.infrastructure.redis.RedisStreamManager;
import hoops.processor.processors.GameEvent.GameEventProcessor;
import io.lettuce.core.StreamMessage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameEventStreamConsumer {
    private static final String CONSUMER_GROUP = "game-events-processor";
    private static final String CONSUMER_NAME = "processor-1";

    private final GameEventProcessor gameEventProcessor;
    private final ObjectMapper objectMapper;
    private final RedisStreamManager redisStreamManager;

    @Value("${redis.stream.batch-size:100}")
    private int batchSize;

    @Value("${redis.stream.poll-timeout-ms:1000}")
    private int pollTimeoutMs;

    @Value("${redis.stream.max-errors:10}")
    private int maxConsecutiveErrors;

    private int consecutiveErrors = 0;

    @PostConstruct
    public void init() {
        redisStreamManager.createConsumerGroup(StreamConstants.GAME_EVENTS_STREAM, CONSUMER_GROUP);
    }

    @Scheduled(fixedDelayString = "${redis.stream.poll-interval-ms:1000}")
    public void processGameEvents() {
        // If we exceed error threshold, stop processing temporarily
        if (consecutiveErrors >= maxConsecutiveErrors) {
            log.error("Too many consecutive errors ({}), pausing stream processing", consecutiveErrors);
            return;
        }
        
        try {
            // Read batch of messages
            List<StreamMessage<String, String>> messages = redisStreamManager.readGroupMessages(
                    StreamConstants.GAME_EVENTS_STREAM,
                    CONSUMER_GROUP,
                    CONSUMER_NAME,
                    batchSize,
                    pollTimeoutMs
            );

            if (messages == null || messages.isEmpty()) {
                return;
            }

            // Process messages
            for (StreamMessage<String, String> message : messages) {
                try {
                    // Check if this is a metadata/init message
                    if (isMetadataMessage(message.getBody())) {
                        log.debug("Skipping metadata message: {}", message.getBody());
                        // Acknowledge metadata message
                        redisStreamManager.acknowledgeMessage(
                                StreamConstants.GAME_EVENTS_STREAM,
                                CONSUMER_GROUP,
                                message.getId()
                        );
                        continue;
                    }
                    
                    // Process actual event
                    GameEvent event = parseGameEvent(message.getBody());
                    gameEventProcessor.processEvent(event);

                    // Acknowledge message
                    redisStreamManager.acknowledgeMessage(
                            StreamConstants.GAME_EVENTS_STREAM,
                            CONSUMER_GROUP,
                            message.getId()
                    );
                    
                    // Reset consecutive errors on success
                    consecutiveErrors = 0;
                } catch (Exception e) {
                    consecutiveErrors++;
                    log.error("Error processing game event: {}, consecutive errors: {}", 
                              message, consecutiveErrors, e);
                    
                    // Acknowledge the message to prevent endless reprocessing of problematic messages
                    // In production, you might want to move these to a dead-letter queue instead
                    redisStreamManager.acknowledgeMessage(
                            StreamConstants.GAME_EVENTS_STREAM,
                            CONSUMER_GROUP,
                            message.getId()
                    );
                }
            }
        } catch (Exception e) {
            consecutiveErrors++;
            log.error("Error reading from stream, consecutive errors: {}", consecutiveErrors, e);
        }
    }

    private boolean isMetadataMessage(Map<String, String> fields) {
        // Check if this is a metadata message (no game event data)
        if (!fields.containsKey("data")) {
            return true;
        }
        
        // Check if this is a stream initialization message
        return fields.containsKey("type") && "STREAM_INIT".equals(fields.get("type"));
    }

    private GameEvent parseGameEvent(Map<String, String> fields) {
        try {
            String data = fields.get("data");
            if (data == null) {
                throw new IllegalArgumentException("Message does not contain 'data' field");
            }
            return objectMapper.readValue(data, GameEvent.class);
        } catch (Exception e) {
            log.error("Error parsing game event: {}", fields, e);
            throw new RuntimeException("Failed to parse game event", e);
        }
    }
}