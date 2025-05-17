package hoops.api.services.teams;

import hoops.api.config.TestRedisConfig;
import hoops.api.config.TestTimescaleDBConfig;
import hoops.api.mappers.TeamMapper;
import hoops.api.models.dtos.teams.TeamMetaDTO;
import hoops.api.models.dtos.teams.TeamStatsDTO;
import hoops.api.models.entities.teams.Team;
import hoops.api.models.entities.teams.TeamStats;
import hoops.api.repositories.teams.TeamsRepository;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = { TestRedisConfig.class, TestTimescaleDBConfig.class })
@Testcontainers
@ActiveProfiles("test")
class TeamsServiceImplTest {
    private static final Logger log = LoggerFactory.getLogger(TeamsServiceImplTest.class);

    @MockBean
    private TeamsRepository teamsRepository;

    @Autowired
    private TeamsServiceImpl teamsService;

    @Autowired
    private TeamMapper teamMapper;

    @Autowired
    private RedisClient redisClient;

    @BeforeEach
    void setUp() {
        // Set a shorter TTL for testing
        ReflectionTestUtils.setField(teamsService, "redisTtl", 60L);

        // Clear Redis before each test
        try (var connection = redisClient.connect()) {
            RedisCommands<String, String> commands = connection.sync();
            commands.flushdb();
        } catch (Exception e) {
            log.error("Error flushing Redis: {}", e.getMessage());
        }
    }

    @Test
    void getTeamStats_CacheMiss_ShouldFetchFromRepository() {
        // Arrange
        String teamId = "team123";
        String seasonId = "season2023";
        String cacheKey = RedisKeyUtil.getTeamStatsKey(teamId, seasonId);

        // Mock repository response
        TeamStats teamStats = createMockTeamStats(teamId, seasonId);
        when(teamsRepository.getTeamStats(teamId, seasonId)).thenReturn(teamStats);

        // Act
        TeamStatsDTO result = teamsService.getTeamStats(teamId, seasonId);

        // Assert
        assertNotNull(result);
        assertEquals(teamId, result.getTeamId());
        assertEquals(seasonId, result.getSeasonId());

        // Verify repository and mapper were called
        verify(teamsRepository, times(1)).getTeamStats(teamId, seasonId);

        // Verify data was cached in Redis
        try (var connection = redisClient.connect()) {
            RedisCommands<String, String> commands = connection.sync();
            String cachedJson = commands.get(cacheKey);
            assertNotNull(cachedJson);
            assertTrue(cachedJson.contains(teamId));
        }
    }

    @Test
    void getTeamStats_CacheHit_ShouldUseCache() {
        // Arrange
        String teamId = "team456";
        String seasonId = "season2023";
        String cacheKey = RedisKeyUtil.getTeamStatsKey(teamId, seasonId);

        // Pre-populate Redis cache
        String cachedJson = "{\"teamId\":\"team456\",\"seasonId\":\"season2023\",\"ppg\":105.8,\"rpg\":45.2,\"apg\":25.1}";
        try (var connection = redisClient.connect()) {
            RedisCommands<String, String> commands = connection.sync();
            commands.setex(cacheKey, 60, cachedJson);
        }

        // Act
        TeamStatsDTO result = teamsService.getTeamStats(teamId, seasonId);

        // Assert
        assertNotNull(result);
        assertEquals(teamId, result.getTeamId());
        assertEquals(seasonId, result.getSeasonId());
        assertEquals(105.8, result.getPpg());
        assertEquals(45.2, result.getRpg());
        assertEquals(25.1, result.getApg());

        // Verify repository and mapper were not called
        verify(teamsRepository, never()).getTeamStats(anyString(), anyString());
    }

    @Test
    void getTeamStats_RepositoryReturnsNull_ShouldReturnNull() {
        // Arrange
        String teamId = "nonexistent";
        String seasonId = "season2023";

        // Mock repository to return null
        when(teamsRepository.getTeamStats(teamId, seasonId)).thenReturn(null);

        // Act
        TeamStatsDTO result = teamsService.getTeamStats(teamId, seasonId);

        // Assert
        assertNull(result);
        verify(teamsRepository, times(1)).getTeamStats(teamId, seasonId);
    }

    @Test
    void getTeamStats_MultipleCalls_ShouldUseCache() {
        // Arrange
        String teamId = "team789";
        String seasonId = "season2023";

        // Mock repository response
        TeamStats teamStats = createMockTeamStats(teamId, seasonId);
        when(teamsRepository.getTeamStats(teamId, seasonId)).thenReturn(teamStats);

        // Act - first call should hit database
        TeamStatsDTO result1 = teamsService.getTeamStats(teamId, seasonId);

        // Act - second call should hit cache
        TeamStatsDTO result2 = teamsService.getTeamStats(teamId, seasonId);

        // Assert
        assertNotNull(result1);
        assertNotNull(result2);

        // Verify repository was called exactly once
        verify(teamsRepository, times(1)).getTeamStats(teamId, seasonId);
    }

    @Test
    void getAllTeams_ShouldReturnAllTeams() {
        // Arrange
        List<Team> teams = List.of(
                createMockTeam("team1", "Lakers"),
                createMockTeam("team2", "Celtics")
        );
        when(teamsRepository.getAllTeams()).thenReturn(teams);

        // Act
        List<TeamMetaDTO> result = teamsService.getAllTeams();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(teamsRepository, times(1)).getAllTeams();
    }

    private Team createMockTeam(String teamId, String name) {
        Team team = new Team();
        team.setTeamId(teamId);
        team.setName(name);
        team.setLeagueId("nba");
        team.setLeagueName("NBA");
        team.setCountry("USA");
        team.setCity(name.equals("Lakers") ? "Los Angeles" : "Boston");
        team.setDivision("Pacific");
        team.setConference("Western");
        team.setArena(name.equals("Lakers") ? "Crypto.com Arena" : "TD Garden");
        team.setFoundedYear(1947);
        team.setCreatedAt(OffsetDateTime.now());
        team.setLastUpdated(OffsetDateTime.now());
        return team;
    }

    private TeamStats createMockTeamStats(String teamId, String seasonId) {
        TeamStats stats = new TeamStats();
        stats.setTeamId(teamId);
        stats.setSeasonId(seasonId);
        stats.setGames(82);
        stats.setPpg(110.5);
        stats.setRpg(44.3);
        stats.setApg(24.8);
        stats.setSpg(7.2);
        stats.setBpg(5.1);
        stats.setTopg(13.5);
        stats.setMpg(240.0);
        stats.setLastUpdated(OffsetDateTime.now());
        return stats;
    }
}