package hoops.processor.services.playerStatEvents;

import hoops.common.redis.RedisKeyUtil;
import hoops.processor.models.entities.PlayerStatEvent;
import hoops.processor.repositories.PlayerStatEvents.PlayerStatEventsRepository;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerStatEventsServiceImpl implements PlayerStatEventsService {
    private final PlayerStatEventsRepository playerStatEventsRepository;
    private final RedisClient redisClient;

    @Override
    public void save(PlayerStatEvent event) {
        try {
            // First save to database
            playerStatEventsRepository.save(event);
            
            // Then invalidate Redis caches atomically
            invalidateRedisCache(event);
        } catch (Exception e) {
            log.error("Error saving player stat event: {}", event, e);
            throw new RuntimeException("Failed to process player stat event", e);
        }
    }
    
    private void invalidateRedisCache(PlayerStatEvent event) {
        try {
            // Get a connection from the client
            RedisCommands<String, String> commands = redisClient.connect().sync();
            
            // Generate cache keys using RedisKeyUtil
            String playerStatsKey = RedisKeyUtil.getPlayerStatsKey(event.getPlayerId(), event.getSeasonId());
            String teamStatsKey = RedisKeyUtil.getTeamStatsKey(event.getTeamId(), event.getSeasonId());
            
            try {
                // Start a transaction
                commands.multi();
                
                // Queue deletion commands
                commands.del(playerStatsKey);
                commands.del(teamStatsKey);
                
                // Execute transaction
                commands.exec();
                
                log.debug("Successfully invalidated Redis cache for player {} and team {}", 
                    event.getPlayerId(), event.getTeamId());
            } catch (Exception e) {
                commands.discard();
                log.error("Failed to invalidate Redis cache: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to invalidate Redis cache", e);
            } finally {
                // Close the connection
                commands.getStatefulConnection().close();
            }
        } catch (Exception e) {
            log.error("Error accessing Redis: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to access Redis", e);
        }
    }
} 