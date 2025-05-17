package hoops.common.redis;


/**
 * Utility class for generating Redis keys
 */
public final class RedisKeyUtil {
    private RedisKeyUtil() {
        // Prevent instantiation
    }

    /**
     * Generate a Redis key for team metadata
     * @param teamId The team ID
     * @return The Redis key
     */
    public static String getTeamKey(String teamId) {
        return RedisConstants.Team.KEY_PREFIX + ":" + teamId;
    }

    /**
     * Generate a Redis key for player metadata
     * @param playerId The player ID
     * @return The Redis key
     */
    public static String getPlayerKey(String playerId) {
        return RedisConstants.Player.KEY_PREFIX + ":" + playerId;
    }

    /**
     * Generate a Redis key for team stats
     * @param teamId The team ID
     * @param seasonId The season ID
     * @return The Redis key
     */
    public static String getTeamStatsKey(String teamId, String seasonId) {
        return RedisConstants.Team.KEY_PREFIX + ":" + teamId + ":" + seasonId;
    }

    /**
     * Generate a Redis key for player stats
     * @param playerId The player ID
     * @param seasonId The season ID
     * @return The Redis key
     */
    public static String getPlayerStatsKey(String playerId, String seasonId) {
        return RedisConstants.Player.KEY_PREFIX + ":" + playerId + ":" + seasonId;
    }

    /**
     * Get the TTL for a team metadata key
     * @return TTL in seconds
     */
    public static int getTeamTTL() {
        return RedisConstants.Team.TTL;
    }

    /**
     * Get the TTL for a player metadata key
     * @return TTL in seconds
     */
    public static int getPlayerTTL() {
        return RedisConstants.Player.TTL;
    } 
    
} 