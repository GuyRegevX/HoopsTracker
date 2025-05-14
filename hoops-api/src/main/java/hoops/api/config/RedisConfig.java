package hoops.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;

@Configuration
public class RedisConfig {
    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);
    
    @Value("${redis.host}")
    private String redisHost;
    
    @Value("${redis.port}")
    private int redisPort;
    
    @Value("${redis.password:}")
    private String redisPassword;
    
    @Value("${redis.timeout:2000}")
    private int timeout;
    
    @Value("${redis.pool.max-total:50}")
    private int maxTotal;
    
    @Value("${redis.pool.max-idle:10}")
    private int maxIdle;
    
    @Value("${redis.pool.min-idle:5}")
    private int minIdle;
    
    @Value("${redis.pool.max-wait-millis:1000}")
    private long maxWaitMillis;

    @Bean
    public JedisPool jedisPool() {
        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(maxTotal);
            poolConfig.setMaxIdle(maxIdle);
            poolConfig.setMinIdle(minIdle);
            poolConfig.setMaxWaitMillis(maxWaitMillis);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            poolConfig.setTestWhileIdle(true);
            
            if (redisPassword != null && !redisPassword.isEmpty()) {
                return new JedisPool(poolConfig, redisHost, redisPort, timeout, redisPassword);
            } else {
                return new JedisPool(poolConfig, redisHost, redisPort, timeout);
            }
        } catch (Exception e) {
            logger.error("Failed to create Redis connection pool", e);
            throw new JedisException("Could not initialize Redis connection pool", e);
        }
    }
} 