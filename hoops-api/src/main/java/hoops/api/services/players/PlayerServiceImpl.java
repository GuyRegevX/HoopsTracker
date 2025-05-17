package hoops.api.services.players;

import hoops.api.mappers.PlayerMapper;
import hoops.api.models.dtos.players.PlayerMetaDTO;
import hoops.api.models.dtos.players.PlayerStatsDTO;
import hoops.api.models.entities.players.Player;
import hoops.api.models.entities.players.PlayerStats;
import hoops.api.repositories.players.PlayerRepository;
import hoops.common.redis.RedisKeyUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class PlayerServiceImpl implements PlayerService {
    private static final Logger log = LoggerFactory.getLogger(PlayerServiceImpl.class);

    private final PlayerRepository playerRepository;
    private final PlayerMapper playerMapper;

    @Autowired(required = false)
    private final RedisClient redisClient;

    @Value("${redis.stats.ttl:3600}")
    private long redisTtl; // Default to 1 hour if not specified

    private final ObjectMapper objectMapper;

    @Override
    public List<PlayerMetaDTO> getAllPlayers() {
        log.info("Fetching all players metadata");
        List<Player> players = playerRepository.getAllPlayers();
        return players.stream()
                .map(playerMapper::toPlayerMetaDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PlayerStatsDTO getPlayerStats(String playerId, String seasonId) {
        log.info("Fetching stats for player {} in season {}", playerId, seasonId);

        // Try to get from Redis first if available
        if (redisClient != null) {
            PlayerStatsDTO cachedStats = getFromRedis(playerId, seasonId);
            if (cachedStats != null) {
                log.info("Cache hit: Returning cached stats for player {} in season {}", playerId, seasonId);
                return cachedStats;
            }
        }

        // Cache miss or Redis not available, get from database
        log.info("Cache miss: Fetching stats from database for player {} in season {}", playerId, seasonId);
        PlayerStats stats = playerRepository.getPlayerStats(playerId, seasonId);

        if (stats == null) {
            log.info("No stats found for player {} in season {}", playerId, seasonId);
            return null;
        }

        PlayerStatsDTO result = playerMapper.toPlayerStatsDTO(stats);

        // Cache the result in Redis if available
        if (redisClient != null) {
            saveToRedis(playerId, seasonId, result);
        }

        return result;
    }

    private PlayerStatsDTO getFromRedis(String playerId, String seasonId) {
        String key = RedisKeyUtil.getPlayerStatsKey(playerId, seasonId);

        try (var connection = redisClient.connect()) {
            RedisCommands<String, String> commands = connection.sync();
            String json = commands.get(key);

            if (json != null) {
                return objectMapper.readValue(json, PlayerStatsDTO.class);
            }
        } catch (Exception e) {
            log.error("Error retrieving player stats from Redis: {}", e.getMessage(), e);
        }

        return null;
    }

    private void saveToRedis(String playerId, String seasonId, PlayerStatsDTO stats) {
        String key = RedisKeyUtil.getPlayerStatsKey(playerId, seasonId);

        try (var connection = redisClient.connect()) {
            RedisCommands<String, String> commands = connection.sync();
            String json = objectMapper.writeValueAsString(stats);

            commands.setex(key, redisTtl, json);
            log.debug("Cached player stats for player {} in season {}", playerId, seasonId);
        } catch (Exception e) {
            log.error("Error caching player stats in Redis: {}", e.getMessage(), e);
        }
    }
}