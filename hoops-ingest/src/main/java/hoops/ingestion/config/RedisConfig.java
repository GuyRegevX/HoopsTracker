package hoops.ingestion.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
public class RedisConfig {
    @Value("${spring.redis.host}")
    private String redisHost;
    
    @Value("${spring.redis.port}")
    private int redisPort;

    @Bean
    public JedisPool jedisPool() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        // Maximum number of connections in the pool
        poolConfig.setMaxTotal(10);
        // Maximum number of idle connections in the pool
        poolConfig.setMaxIdle(5);
        // Minimum number of idle connections to maintain
        poolConfig.setMinIdle(2);
        // Whether to test connections on borrow
        poolConfig.setTestOnBorrow(true);
        // Whether to test connections while idle
        poolConfig.setTestWhileIdle(true);
        // Maximum time a connection can be idle before being evicted
        poolConfig.setMinEvictableIdleTimeMillis(300000); // 5 minutes
        // Time between eviction runs
        poolConfig.setTimeBetweenEvictionRunsMillis(60000); // 1 minute
        
        // Create pool with timeout settings
        return new JedisPool(
            poolConfig,
            redisHost,
            redisPort,
            2000, // connection timeout: 2 seconds
            null, // password
            0,    // database
            null  // client name
        );
    }
} 