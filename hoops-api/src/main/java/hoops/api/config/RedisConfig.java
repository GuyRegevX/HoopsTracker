package hoops.api.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;


import java.time.Duration;


@Configuration
@Profile("!test")
public class RedisConfig {
    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${redis.host}")
    private String redisHost;

    @Value("${redis.port}")
    private int redisPort;

    @Value("${redis.password:}")
    private String redisPassword;

    @Value("${redis.database:0}")
    private int database;

    @Value("${redis.timeout:2000}")
    private int timeout;

    @Value("${redis.client.thread-pool-size:4}")
    private int threadPoolSize;

    @Bean(destroyMethod = "shutdown")
    public ClientResources clientResources() {
        return DefaultClientResources.builder()
                .ioThreadPoolSize(threadPoolSize)
                .computationThreadPoolSize(threadPoolSize)
                .build();
    }

    @Bean(destroyMethod = "shutdown")
    public RedisClient redisClient(ClientResources clientResources) {
        try {
            RedisURI.Builder uriBuilder = RedisURI.builder()
                    .withHost(redisHost)
                    .withPort(redisPort)
                    .withDatabase(database)
                    .withTimeout(Duration.ofMillis(timeout));

            if (redisPassword != null && !redisPassword.isEmpty()) {
                uriBuilder.withPassword(redisPassword.toCharArray());
            }

            RedisURI redisURI = uriBuilder.build();

            ClientOptions options = ClientOptions.builder()
                    .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                    .autoReconnect(true)
                    .build();

            RedisClient client = RedisClient.create(clientResources, redisURI);
            client.setOptions(options);

            // Test the connection to ensure it's properly configured
            client.connect().sync().ping();
            logger.info("Successfully connected to Redis at {}:{}", redisHost, redisPort);

            return client;
        } catch (Exception e) {
            logger.error("Failed to create Redis client", e);
            throw new RuntimeException("Could not initialize Redis connection", e);
        }
    }
} 