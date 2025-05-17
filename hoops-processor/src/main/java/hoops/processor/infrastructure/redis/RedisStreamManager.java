package hoops.processor.infrastructure.redis;

import io.lettuce.core.*;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStreamManager {
    private final RedisClient redisClient;
    private RedisCommands<String, String> commands;

    private RedisCommands<String, String> getCommands() {
        if (commands == null) {
            commands = redisClient.connect().sync();
        }
        return commands;
    }

    public void createConsumerGroup(String stream, String groupName) {
        try {
            // Create stream if it doesn't exist
            if (!streamExists(stream)) {
                getCommands().xadd(stream, Map.of("init", "true"));
                log.info("Created stream: {}", stream);
            }

            // Try to create consumer group
            try {
                getCommands().xgroupCreate(
                        XReadArgs.StreamOffset.from(stream, "0-0"),
                        groupName,
                        XGroupCreateArgs.Builder.mkstream()
                );
                log.info("Created consumer group: {}", groupName);
            } catch (RedisBusyException e) {
                // This specific exception means "BUSYGROUP Consumer Group name already exists"
                // which is expected when the group already exists
                log.info("Consumer group already exists: {}", groupName);
            } catch (Exception e) {
                // Other unexpected exceptions should be re-thrown
                log.error("Error creating consumer group: {} for stream: {}", groupName, stream, e);
                throw e;
            }
        } catch (Exception e) {
            log.error("Error creating consumer group: {} for stream: {}", groupName, stream, e);
            throw e;
        }
    }

    private boolean streamExists(String stream) {
        try {
            getCommands().xinfoStream(stream);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public List<StreamMessage<String, String>> readGroupMessages(
            String stream,
            String group,
            String consumer,
            int count,
            long blockMillis
    ) {
        return getCommands().xreadgroup(
                io.lettuce.core.Consumer.from(group, consumer),
                XReadArgs.Builder.count(count).block(blockMillis),
                XReadArgs.StreamOffset.lastConsumed(stream)
        );
    }

    public void acknowledgeMessage(String stream, String group, String messageId) {
        getCommands().xack(stream, group, messageId);
    }
}