package hoops.processor.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@TestConfiguration
public class TestRedisConfig {
    @Bean
    @ServiceConnection
    public GenericContainer<?> redisContainer() {
        GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);
        redis.start();
        return redis;
    }

    @Bean
    public JedisPool jedisPool(GenericContainer<?> redisContainer) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setTestOnBorrow(true);
        
        return new JedisPool(
            poolConfig,
            redisContainer.getHost(),
            redisContainer.getFirstMappedPort(),
            2000 // timeout
        );
    }
} 