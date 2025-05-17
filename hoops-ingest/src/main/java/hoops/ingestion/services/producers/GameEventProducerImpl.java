package hoops.ingestion.services.producers;

import com.fasterxml.jackson.databind.ObjectMapper;
import hoops.common.constants.StreamConstants;
import hoops.common.models.events.GameEvent;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameEventProducerImpl implements GameEventProducer {
    private final RedisClient redisClient;
    private final ObjectMapper objectMapper;
    private volatile boolean isStreamInitialized = false;

    @PostConstruct
    public void initStream() {
        try {
            RedisCommands<String, String> commands = redisClient.connect().sync();
            try {
                if (!streamExists(commands, StreamConstants.GAME_EVENTS_STREAM)) {
                    // Initialize stream with metadata
                    Map<String, String> metadata = Map.of(
                            "type", "STREAM_INIT",
                            "service", "game-event-ingest",
                            "timestamp", Instant.now().toString()
                    );
                    String entryId = commands.xadd(
                            StreamConstants.GAME_EVENTS_STREAM,
                            metadata
                    );
                    log.info("Initialized game events stream with ID: {}", entryId);
                }
                isStreamInitialized = true;
            } finally {
                commands.getStatefulConnection().close();
            }
        } catch (Exception e) {
            log.error("Failed to initialize stream", e);
            throw new RuntimeException("Stream initialization failed", e);
        }
    }

    @Override
    public void publishEvent(GameEvent event) {
        if (!isStreamInitialized) {
            initStream();
        }

        RedisCommands<String, String> commands = null;
        try {
            commands = redisClient.connect().sync();
            String eventJson = objectMapper.writeValueAsString(event);
            String entryId = commands.xadd(
                    StreamConstants.GAME_EVENTS_STREAM,
                    Map.of("data", eventJson)
            );
            log.debug("Published event {} with ID {}", event, entryId);
        } catch (Exception e) {
            log.error("Failed to publish event: {}", event, e);
            throw new RuntimeException("Event publishing failed", e);
        } finally {
            if (commands != null) {
                commands.getStatefulConnection().close();
            }
        }
    }

    private boolean streamExists(RedisCommands<String, String> commands, String key) {
        try {
            commands.xinfoStream(key);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}