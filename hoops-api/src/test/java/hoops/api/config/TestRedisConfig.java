package hoops.api.config;

import java.time.Duration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;

@TestConfiguration
public class TestRedisConfig {
    
    private static final int REDIS_PORT = 6379;
    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");
    private static final int DATABASE = 0;
    private static final int TIMEOUT = 2000;
    private static final int THREAD_POOL_SIZE = 4;
    
    @Bean
    @ServiceConnection(name = "redis")
    public GenericContainer<?> redisContainer() {
        GenericContainer<?> redis = new GenericContainer<>(REDIS_IMAGE)
            .withExposedPorts(REDIS_PORT)
            .withCommand("redis-server --port " + REDIS_PORT)
            .withReuse(true);
        redis.start();
        return redis;
    }
    
    @Bean(destroyMethod = "shutdown")
    public ClientResources clientResources() {
        return DefaultClientResources.builder()
                .ioThreadPoolSize(THREAD_POOL_SIZE)
                .computationThreadPoolSize(THREAD_POOL_SIZE)
                .build();
    }
    
    @Bean(destroyMethod = "shutdown")
    public RedisClient redisClient(GenericContainer<?> redisContainer, ClientResources clientResources) {
        String host = redisContainer.getHost();
        Integer mappedPort = redisContainer.getMappedPort(REDIS_PORT);

        RedisURI redisURI = RedisURI.builder()
                .withHost(host)
                .withPort(mappedPort)
                .withDatabase(DATABASE)
                .withTimeout(Duration.ofMillis(TIMEOUT))
                .build();

        ClientOptions options = ClientOptions.builder()
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .autoReconnect(true)
                .build();
                
        RedisClient client = RedisClient.create(clientResources, redisURI);
        client.setOptions(options);
        
        return client;
    }
} 