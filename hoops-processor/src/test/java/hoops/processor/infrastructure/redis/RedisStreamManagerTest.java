package hoops.processor.infrastructure.redis;

import hoops.processor.config.TestRedisConfig;
import io.lettuce.core.Consumer;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisBusyException;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {RedisStreamManager.class})
@Import(TestRedisConfig.class)
@ActiveProfiles("test")
class RedisStreamManagerTest {

    private static final String TEST_STREAM = "test-stream";
    private static final String TEST_GROUP = "test-group";
    private static final String TEST_CONSUMER = "test-consumer";

    @Autowired
    private RedisStreamManager redisStreamManager;

    @Autowired
    private RedisClient redisClient;

    private RedisCommands<String, String> commands;

    @BeforeEach
    void setUp() {
        commands = redisClient.connect().sync();
        commands.flushall();
    }

    @Test
    void shouldCreateConsumerGroup() {
        // When
        redisStreamManager.createConsumerGroup(TEST_STREAM, TEST_GROUP);

        // Then
        List<Object> groups = commands.xinfoGroups(TEST_STREAM);
        assertThat(groups).isNotEmpty();
        
        // Directly check if the group exists by trying to use it
        // This is a more reliable approach as it tests functionality rather than implementation
        boolean groupWorks = true;
        try {
            // If we can add a message and read it with the group, the group exists
            String messageId = commands.xadd(TEST_STREAM, Map.of("test", "value"));
            var messages = commands.xreadgroup(
                Consumer.from(TEST_GROUP, "test-consumer"),
                XReadArgs.Builder.count(1),
                XReadArgs.StreamOffset.lastConsumed(TEST_STREAM)
            );
            assertThat(messages).isNotEmpty();
        } catch (Exception e) {
            groupWorks = false;
        }
        
        assertThat(groupWorks).isTrue();
    }

    @Test
    void shouldNotCreateDuplicateConsumerGroup() {
        // Given
        redisStreamManager.createConsumerGroup(TEST_STREAM, TEST_GROUP);

        // When - this should not throw exception
        redisStreamManager.createConsumerGroup(TEST_STREAM, TEST_GROUP);
    
        // Then
        var groups = commands.xinfoGroups(TEST_STREAM);
        assertThat(groups).isNotEmpty();
        
        // Verify only one group exists
        long groupCount = groups.size();
        assertThat(groupCount).isEqualTo(1);
        
        // Verify the group exists by testing functionality
        // This is more reliable than inspecting internal structure which can vary
        boolean groupWorks = false;
        try {
            // Add a message to the stream
            commands.xadd(TEST_STREAM, Map.of("test", "verify"));
            
            // Try to read it with the consumer group - this will only succeed if the group exists
            var messages = commands.xreadgroup(
                Consumer.from(TEST_GROUP, TEST_CONSUMER),
                XReadArgs.Builder.count(1),
                XReadArgs.StreamOffset.lastConsumed(TEST_STREAM)
            );
            
            groupWorks = messages != null && !messages.isEmpty();
        } catch (Exception e) {
            // If we get an exception, the group doesn't work correctly
            groupWorks = false;
        }
        
        assertThat(groupWorks).isTrue();
    }

    @Test
    void shouldReadMessages() {
        // Given
        redisStreamManager.createConsumerGroup(TEST_STREAM, TEST_GROUP);
        
        // Read and acknowledge the initialization message first
        List<StreamMessage<String, String>> initMessages = redisStreamManager.readGroupMessages(
                TEST_STREAM,
                TEST_GROUP,
                TEST_CONSUMER,
                1,
                100
        );
        
        // There should be one init message
        assertThat(initMessages).isNotEmpty();
        // Acknowledge the init message
        for (StreamMessage<String, String> msg : initMessages) {
            commands.xack(TEST_STREAM, TEST_GROUP, msg.getId());
        }
        
        // Add our test message to the stream
        Map<String, String> message = Map.of("key", "value");
        String messageId = commands.xadd(TEST_STREAM, message);
    
        // When - read new messages
        List<StreamMessage<String, String>> result = redisStreamManager.readGroupMessages(
                TEST_STREAM,
                TEST_GROUP,
                TEST_CONSUMER,
                1,
                100
        );

        // Then
        assertThat(result).isNotEmpty();
        StreamMessage<String, String> streamMessage = result.get(0);
        assertThat(streamMessage.getBody()).containsAllEntriesOf(message);
    }

    @Test
    void shouldAcknowledgeMessages() {
        // Given
        redisStreamManager.createConsumerGroup(TEST_STREAM, TEST_GROUP);
        
        // First read and acknowledge the initialization message
        List<StreamMessage<String, String>> initMessages = commands.xreadgroup(
            Consumer.from(TEST_GROUP, TEST_CONSUMER),
            XReadArgs.Builder.count(1),
            XReadArgs.StreamOffset.lastConsumed(TEST_STREAM)
        );
        
        // Acknowledge the init message
        if (!initMessages.isEmpty()) {
            String initMessageId = initMessages.get(0).getId();
            commands.xack(TEST_STREAM, TEST_GROUP, initMessageId);
        }
        
        // Now add our test message
        String messageId = commands.xadd(TEST_STREAM, Map.of("key", "value"));
        
        // Read message to make it pending
        List<StreamMessage<String, String>> messages = commands.xreadgroup(
            Consumer.from(TEST_GROUP, TEST_CONSUMER),
            XReadArgs.Builder.count(1),
            XReadArgs.StreamOffset.lastConsumed(TEST_STREAM)
        );
        
        // Verify message is pending
        long pendingCount = commands.xpending(TEST_STREAM, TEST_GROUP).getCount();
        assertThat(pendingCount).isEqualTo(1);
        
        // When
        redisStreamManager.acknowledgeMessage(TEST_STREAM, TEST_GROUP, messageId);
        
        // Then
        pendingCount = commands.xpending(TEST_STREAM, TEST_GROUP).getCount();
        assertThat(pendingCount).isEqualTo(0);
    }

    @Test
    void shouldCreateStreamIfNotExists() {
        // When
        redisStreamManager.createConsumerGroup(TEST_STREAM, TEST_GROUP);

        // Then
        var streamInfo = commands.xinfoStream(TEST_STREAM);
        assertThat(streamInfo).isNotNull();
    }
}