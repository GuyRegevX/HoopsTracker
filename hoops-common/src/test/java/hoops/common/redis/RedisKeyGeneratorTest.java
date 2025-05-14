package hoops.common.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static hoops.common.redis.RedisConstants.*;

import java.util.UUID;

class RedisKeyGeneratorTest {
    
    private RedisKeyGenerator keyGenerator;
    private final UUID TEST_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private final String TEST_UUID_STRING = "123e4567-e89b-12d3-a456-426614174000";
    
    @BeforeEach
    void setUp() {
        keyGenerator = new RedisKeyGenerator();
    }
    
    @Test
    void generateTeamKey_WithUUID_ShouldReturnCorrectFormat() {
        // When
        String key = keyGenerator.generateTeamKey(TEST_UUID);
        
        // Then
        assertEquals("team:" + TEST_UUID_STRING, key);
    }
    
    @Test
    void generateTeamKey_WithString_ShouldReturnCorrectFormat() {
        // When
        String key = keyGenerator.generateTeamKey(TEST_UUID_STRING);
        
        // Then
        assertEquals("team:" + TEST_UUID_STRING, key);
    }
    
    @Test
    void generatePlayerKey_WithUUID_ShouldReturnCorrectFormat() {
        // When
        String key = keyGenerator.generatePlayerKey(TEST_UUID);
        
        // Then
        assertEquals("player:" + TEST_UUID_STRING, key);
    }
    
    @Test
    void generateTeamStatsKey_WithUUID_ShouldReturnCorrectFormat() {
        // Given
        UUID seasonId = UUID.randomUUID();
        
        // When
        String key = keyGenerator.generateTeamStatsKey(TEST_UUID, seasonId);
        
        // Then
        assertEquals("team:stats:" + TEST_UUID_STRING + ":" + seasonId.toString(), key);
    }
    
    @Test
    void generatePlayerStatsKey_WithUUID_ShouldReturnCorrectFormat() {
        // Given
        UUID seasonId = UUID.randomUUID();
        
        // When
        String key = keyGenerator.generatePlayerStatsKey(TEST_UUID, seasonId);
        
        // Then
        assertEquals("player:stats:" + TEST_UUID_STRING + ":" + seasonId.toString(), key);
    }

    @Test
    void generateGameKey_WithUUID_ShouldReturnCorrectFormat() {
        // When
        String key = keyGenerator.generateGameKey(TEST_UUID);
        
        // Then
        assertEquals("game:" + TEST_UUID_STRING, key);
    }
    
    @Test
    void shouldThrowException_WhenInvalidUUIDString() {
        // Given
        String invalidUUID = "not-a-uuid";
        
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> 
            keyGenerator.generateTeamKey(invalidUUID));
    }
    
    @Test
    void shouldThrowException_WhenNullId() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> 
            keyGenerator.generateTeamKey(null));
    }
    
    @Test
    void shouldThrowException_WhenInvalidType() {
        // Given
        Integer invalidType = 123;
        
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> 
            keyGenerator.generateTeamKey(invalidType));
    }
    
    @Test
    void extractTeamId_ShouldReturnCorrectId() {
        // Given
        String teamKey = "team:" + TEST_UUID_STRING;
        String teamStatsKey = "team:stats:" + TEST_UUID_STRING + ":2024";
        
        // When
        String teamId1 = keyGenerator.extractTeamId(teamKey);
        String teamId2 = keyGenerator.extractTeamId(teamStatsKey);
        
        // Then
        assertEquals(TEST_UUID_STRING, teamId1);
        assertEquals(TEST_UUID_STRING, teamId2);
    }
    
    @Test
    void extractPlayerId_ShouldReturnCorrectId() {
        // Given
        String playerKey = "player:" + TEST_UUID_STRING;
        String playerStatsKey = "player:stats:" + TEST_UUID_STRING + ":2024";
        
        // When
        String playerId1 = keyGenerator.extractPlayerId(playerKey);
        String playerId2 = keyGenerator.extractPlayerId(playerStatsKey);
        
        // Then
        assertEquals(TEST_UUID_STRING, playerId1);
        assertEquals(TEST_UUID_STRING, playerId2);
    }
    
    @Test
    void extractSeasonId_ShouldReturnCorrectId() {
        // Given
        String seasonId = UUID.randomUUID().toString();
        String teamStatsKey = "team:stats:" + TEST_UUID_STRING + ":" + seasonId;
        String playerStatsKey = "player:stats:" + TEST_UUID_STRING + ":" + seasonId;
        
        // When
        String seasonId1 = keyGenerator.extractSeasonId(teamStatsKey);
        String seasonId2 = keyGenerator.extractSeasonId(playerStatsKey);
        
        // Then
        assertEquals(seasonId, seasonId1);
        assertEquals(seasonId, seasonId2);
    }
    
    @Test
    void getTTLForKey_ShouldReturnCorrectTTL() {
        // Given
        String teamKey = "team:" + TEST_UUID_STRING;
        String playerKey = "player:" + TEST_UUID_STRING;
        String teamStatsKey = "team:stats:" + TEST_UUID_STRING + ":" + UUID.randomUUID();
        String playerStatsKey = "player:stats:" + TEST_UUID_STRING + ":" + UUID.randomUUID();
        
        // When/Then
        assertEquals(TEAM_TTL, keyGenerator.getTTLForKey(teamKey));
        assertEquals(PLAYER_TTL, keyGenerator.getTTLForKey(playerKey));
        assertEquals(TEAM_STATS_TTL, keyGenerator.getTTLForKey(teamStatsKey));
        assertEquals(PLAYER_STATS_TTL, keyGenerator.getTTLForKey(playerStatsKey));
        assertEquals(-1, keyGenerator.getTTLForKey("invalid:key"));
        assertEquals(-1, keyGenerator.getTTLForKey(null));
    }
} 