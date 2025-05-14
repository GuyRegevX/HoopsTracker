package hoops.common.redis;

import org.springframework.stereotype.Component;
import static hoops.common.redis.RedisConstants.*;
import java.util.UUID;

/**
 * Utility class for generating Redis keys
 */
@Component
public class RedisKeyGenerator {

    /**
     * Generate a Redis key for team metadata
     * @param id The team ID (String or UUID)
     * @return The Redis key with team prefix
     */
    public String generateTeamKey(Object id) {
        return Team.KEY_PREFIX + convertToString(id);
    }

    /**
     * Generate a Redis key for player metadata
     * @param id The player ID (String or UUID)
     * @return The Redis key with player prefix
     */
    public String generatePlayerKey(Object id) {
        return Player.KEY_PREFIX + convertToString(id);
    }

    /**
     * Generate a Redis key for team stats
     * @param teamId The team ID (String or UUID)
     * @param seasonId The season ID (String or UUID)
     * @return The Redis key with team stats prefix
     */
    public String generateTeamStatsKey(Object teamId, Object seasonId) {
        return Team.STATS_KEY_PREFIX + convertToString(teamId) + ":" + convertToString(seasonId);
    }

    /**
     * Generate a Redis key for player stats
     * @param playerId The player ID (String or UUID)
     * @param seasonId The season ID (String or UUID)
     * @return The Redis key with player stats prefix
     */
    public String generatePlayerStatsKey(Object playerId, Object seasonId) {
        return Player.STATS_KEY_PREFIX + convertToString(playerId) + ":" + convertToString(seasonId);
    }

    /**
     * Generate a Redis key for game data
     * @param gameId The game ID (String or UUID)
     * @return The Redis key with game prefix
     */
    public String generateGameKey(Object gameId) {
        return "game:" + convertToString(gameId);
    }

    /**
     * Convert UUID or String to string format
     * @param id The ID (can be UUID or String)
     * @return String representation of the ID
     */
    private String convertToString(Object id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        
        if (id instanceof UUID) {
            return id.toString();
        }
        
        if (id instanceof String) {
            try {
                // Try to parse as UUID to validate format
                UUID.fromString((String) id);
                return (String) id;
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid UUID string: " + id);
            }
        }
        
        throw new IllegalArgumentException("ID must be either UUID or String");
    }

    /**
     * Extract team ID from a Redis key
     * @param key The Redis key
     * @return The team ID or null if key doesn't match pattern
     */
    public String extractTeamId(String key) {
        if (key == null) return null;
        
        if (key.startsWith(Team.KEY_PREFIX)) {
            return key.substring(Team.KEY_PREFIX.length());
        }
        
        if (key.startsWith(Team.STATS_KEY_PREFIX)) {
            String[] parts = key.split(":");
            if (parts.length >= 3) {
                return parts[2];
            }
        }
        
        return null;
    }

    /**
     * Extract player ID from a Redis key
     * @param key The Redis key
     * @return The player ID or null if key doesn't match pattern
     */
    public String extractPlayerId(String key) {
        if (key == null) return null;
        
        if (key.startsWith(Player.KEY_PREFIX)) {
            return key.substring(Player.KEY_PREFIX.length());
        }
        
        if (key.startsWith(Player.STATS_KEY_PREFIX)) {
            String[] parts = key.split(":");
            if (parts.length >= 3) {
                return parts[2];
            }
        }
        
        return null;
    }

    /**
     * Extract season ID from a Redis stats key
     * @param key The Redis key
     * @return The season ID or null if key doesn't match pattern
     */
    public String extractSeasonId(String key) {
        if (key == null) return null;
        
        if (key.startsWith(Team.STATS_KEY_PREFIX) || 
            key.startsWith(Player.STATS_KEY_PREFIX)) {
            String[] parts = key.split(":");
            if (parts.length >= 4) {
                return parts[3];
            }
        }
        
        return null;
    }

    /**
     * Get TTL for a Redis key based on its type
     * @param key The Redis key
     * @return TTL in seconds, or -1 if key type is not recognized
     */
    public int getTTLForKey(String key) {
        if (key == null) return -1;
        
        if (key.startsWith(Team.KEY_PREFIX) && !key.startsWith(Team.STATS_KEY_PREFIX)) {
            return Team.TTL;
        }
        
        if (key.startsWith(Player.KEY_PREFIX) && !key.startsWith(Player.STATS_KEY_PREFIX)) {
            return Player.TTL;
        }
        
        if (key.startsWith(Team.STATS_KEY_PREFIX)) {
            return Team.STATS_TTL;
        }
        
        if (key.startsWith(Player.STATS_KEY_PREFIX)) {
            return Player.STATS_TTL;
        }
        
        return -1;
    }
} 