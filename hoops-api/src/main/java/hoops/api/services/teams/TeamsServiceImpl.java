package hoops.api.services.teams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import hoops.api.mappers.TeamMapper;
import hoops.api.models.dtos.teams.TeamMetaDTO;
import hoops.api.models.dtos.teams.TeamStatsDTO;
import hoops.api.models.entities.teams.Team;
import hoops.api.models.entities.teams.TeamStats;
import hoops.api.repositories.teams.TeamsRepository;
import hoops.common.redis.RedisKeyUtil;
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
public class TeamsServiceImpl implements TeamsService {
    private static final Logger log = LoggerFactory.getLogger(TeamsServiceImpl.class);

    private final TeamsRepository teamsRepository;
    private final TeamMapper teamMapper;
    private final RedisClient redisClient;
    private final ObjectMapper objectMapper;

    @Value("${redis.stats.ttl:3600}")
    private long redisTtl; //

    @Override
    public List<TeamMetaDTO> getAllTeams() {
        List<Team> teams = teamsRepository.getAllTeams();
        return teams.stream()
                .map(teamMapper::toTeamMetaDTO)
                .collect(Collectors.toList());
    }

    @Override
    public TeamStatsDTO getTeamStats(String teamId, String seasonId) {
        log.info("Getting team stats for team {} in season {}", teamId, seasonId);

        // Try to get from Redis first
        if (redisClient != null) {
            TeamStatsDTO cachedStats = getFromRedis(teamId, seasonId);
            if (cachedStats != null) {
                log.info("Retrieved team stats from Redis cache for team={}, season={}", teamId, seasonId);
                return cachedStats;
            }
        }

        // Get from database if not in cache
        TeamStats stats = teamsRepository.getTeamStats(teamId, seasonId);
        if (stats == null) {
            log.info("No team stats found for team={}, season={}", teamId, seasonId);
            return null;
        }

        // Map to DTO
        TeamStatsDTO result = teamMapper.toTeamStatsDTO(stats);

        // Cache the result in Redis
        if (redisClient != null) {
            saveToRedis(teamId, seasonId, result);
        }

        return result;
    }

    private TeamStatsDTO getFromRedis(String teamId, String seasonId) {
        String key = RedisKeyUtil.getTeamStatsKey(teamId, seasonId);

        try (var connection = redisClient.connect()) {
            RedisCommands<String, String> commands = connection.sync();
            String json = commands.get(key);

            if (json != null) {
                return objectMapper.readValue(json, TeamStatsDTO.class);
            }
        } catch (Exception e) {
            log.error("Error retrieving player stats from Redis: {}", e.getMessage(), e);
        }

        return null;
    }

    private void saveToRedis(String teamId, String seasonId, TeamStatsDTO stats) {
        String key = RedisKeyUtil.getTeamStatsKey(teamId, seasonId);

        try (var connection = redisClient.connect()) {
            RedisCommands<String, String> commands = connection.sync();
            String json = objectMapper.writeValueAsString(stats);

            commands.setex(key, redisTtl, json);
            log.debug("Cached player stats for player {} in season {}", teamId, seasonId);
        } catch (Exception e) {
            log.error("Error caching player stats in Redis: {}", e.getMessage(), e);
        }
    }
}