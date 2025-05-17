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
} 