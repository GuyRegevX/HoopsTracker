package hoops.common.redis;

/**
 * Constants for Redis keys and TTLs
 */
public final class RedisConstants {
    private RedisConstants() {
        // Prevent instantiation
    }

    /**
     * Team-related Redis constants
     */
    public static final class Team {
        private Team() {
            // Prevent instantiation
        }

        // Key prefixes
        public static final String KEY_PREFIX = "team:";
        public static final String STATS_KEY_PREFIX = "team:stats:";

        // TTLs in seconds
        public static final int TTL = 3600;        // 1 hour
        public static final int STATS_TTL = 300;   // 5 minutes
    }

    /**
     * Player-related Redis constants
     */
    public static final class Player {
        private Player() {
            // Prevent instantiation
        }

        // Key prefixes
        public static final String KEY_PREFIX = "player:";
        public static final String STATS_KEY_PREFIX = "player:stats:";

        // TTLs in seconds
        public static final int TTL = 3600;        // 1 hour
        public static final int STATS_TTL = 300;   // 5 minutes
    }
}
   