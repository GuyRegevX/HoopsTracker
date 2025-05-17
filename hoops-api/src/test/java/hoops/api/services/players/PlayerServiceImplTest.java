package hoops.api.services.players;

import hoops.api.config.TestRedisConfig;
import hoops.api.config.TestTimescaleDBConfig;
import hoops.api.models.dtos.players.PlayerStatsDTO;
import hoops.api.models.entities.players.PlayerStats;
import hoops.api.repositories.players.PlayerRepository;
import hoops.common.redis.RedisKeyUtil;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = { TestRedisConfig.class, TestTimescaleDBConfig.class })
@Testcontainers
@ActiveProfiles("test")
class PlayerServiceImplTest {
    private static final Logger log = LoggerFactory.getLogger(PlayerServiceImplTest.class);

    @MockBean
    private PlayerRepository playerRepository;

    @Autowired
    private PlayerServiceImpl playersService;

    @Autowired
    private RedisClient redisClient;

    @BeforeEach
    void setUp() {
        // Set a shorter TTL for testing
        ReflectionTestUtils.setField(playersService, "redisTtl", 60L);

        // Clear Redis before each test
        try (var connection = redisClient.connect()) {
            RedisCommands<String, String> commands = connection.sync();
            commands.flushdb();
        } catch (Exception e) {
            log.error("Error flushing Redis: {}", e.getMessage());
        }
    }

    @Test
    void getPlayerStats_CacheMiss_ShouldFetchFromRepository() {
        // Arrange
        String playerId = "player123";
        String seasonId = "season2023";
        String cacheKey = RedisKeyUtil.getPlayerStatsKey(playerId, seasonId);

        // Mock repository response
        PlayerStats playerStats = createMockPlayerStats(playerId, seasonId);
        when(playerRepository.getPlayerStats(playerId, seasonId)).thenReturn(playerStats);

        // Act
        PlayerStatsDTO result = playersService.getPlayerStats(playerId, seasonId);

        // Assert
        assertNotNull(result);
        assertEquals(playerId, result.getPlayerId());
        assertEquals(seasonId, result.getSeasonId());

        // Verify repository was called
        verify(playerRepository, times(1)).getPlayerStats(playerId, seasonId);

        // Verify data was cached in Redis
        try (var connection = redisClient.connect()) {
            RedisCommands<String, String> commands = connection.sync();
            String cachedJson = commands.get(cacheKey);
            assertNotNull(cachedJson);
            assertTrue(cachedJson.contains(playerId));
        }
    }

    @Test
    void getPlayerStats_CacheHit_ShouldUseCache() {
        // Arrange
        String playerId = "player456";
        String seasonId = "season2023";
        String cacheKey = RedisKeyUtil.getPlayerStatsKey(playerId, seasonId);

        // Pre-populate Redis cache
        String cachedJson = "{\"playerId\":\"player456\",\"seasonId\":\"season2023\",\"ppg\":30.2,\"rpg\":8.1,\"apg\":9.3}";
        try (var connection = redisClient.connect()) {
            RedisCommands<String, String> commands = connection.sync();
            commands.setex(cacheKey, 60, cachedJson);
        }

        // Act
        PlayerStatsDTO result = playersService.getPlayerStats(playerId, seasonId);

        // Assert
        assertNotNull(result);
        assertEquals(playerId, result.getPlayerId());
        assertEquals(seasonId, result.getSeasonId());
        assertEquals(30.2, result.getPpg());
        assertEquals(8.1, result.getRpg());
        assertEquals(9.3, result.getApg());

        // Verify repository was not called
        verify(playerRepository, never()).getPlayerStats(anyString(), anyString());
    }

    @Test
    void getPlayerStats_RepositoryReturnsNull_ShouldReturnNull() {
        // Arrange
        String playerId = "nonexistent";
        String seasonId = "season2023";

        // Mock repository to return null
        when(playerRepository.getPlayerStats(playerId, seasonId)).thenReturn(null);

        // Act
        PlayerStatsDTO result = playersService.getPlayerStats(playerId, seasonId);

        // Assert
        assertNull(result);
        verify(playerRepository, times(1)).getPlayerStats(playerId, seasonId);
    }

    @Test
    void getPlayerStats_MultipleCalls_ShouldUseCache() {
        // Arrange
        String playerId = "player789";
        String seasonId = "season2023";

        // Mock repository response
        PlayerStats playerStats = createMockPlayerStats(playerId, seasonId);
        when(playerRepository.getPlayerStats(playerId, seasonId)).thenReturn(playerStats);

        // Act - first call should hit database
        PlayerStatsDTO result1 = playersService.getPlayerStats(playerId, seasonId);

        // Act - second call should hit cache
        PlayerStatsDTO result2 = playersService.getPlayerStats(playerId, seasonId);

        // Assert
        assertNotNull(result1);
        assertNotNull(result2);

        // Verify repository was called exactly once
        verify(playerRepository, times(1)).getPlayerStats(playerId, seasonId);
    }

    private PlayerStats createMockPlayerStats(String playerId, String seasonId) {
        PlayerStats stats = new PlayerStats();
        stats.setPlayerId(playerId);
        stats.setTeamId("team123");
        stats.setSeasonId(seasonId);
        stats.setGames(82);
        stats.setPpg(25.7);
        stats.setRpg(5.2);
        stats.setApg(7.4);
        stats.setSpg(1.2);
        stats.setBpg(0.7);
        stats.setTopg(3.1);
        stats.setMpg(36.5);
        stats.setLastUpdated(OffsetDateTime.now());
        return stats;
    }
}