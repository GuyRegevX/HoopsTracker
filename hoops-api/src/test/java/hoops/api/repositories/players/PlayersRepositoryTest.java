package hoops.api.repositories.players;

import hoops.api.models.entities.players.Player;
import hoops.api.models.entities.players.PlayerStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayersRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private PlayersRepositoryImpl playersRepository;

    private Player testPlayer;
    private PlayerStats testStats;
    private static final String TEST_PLAYER_ID = "test-player-id";
    private static final String TEST_SEASON_ID = "test-season-id";

    @BeforeEach
    void setUp() {
        // Setup test player
        testPlayer = new Player();
        testPlayer.setPlayerId(TEST_PLAYER_ID);
        testPlayer.setName("Test Player");
        testPlayer.setTeamId("test-team-id");
        testPlayer.setTeamName("Test Team");
        testPlayer.setJerseyNumber(23);
        testPlayer.setPosition("SF");
        testPlayer.setLastUpdated(OffsetDateTime.now(ZoneOffset.UTC));

        // Setup test stats
        testStats = new PlayerStats();
        testStats.setPlayerId(TEST_PLAYER_ID);
        testStats.setSeasonId(TEST_SEASON_ID);
        testStats.setGames(10);
        testStats.setPpg(25.5);
        testStats.setApg(7.2);
        testStats.setRpg(6.3);
        testStats.setSpg(1.4);
        testStats.setBpg(0.8);
        testStats.setTopg(2.1);
        testStats.setMpg(32.5);
        testStats.setLastUpdated(OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Test
    void getAllPlayers_ShouldReturnPlayersList() {
        // Given
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
            .thenReturn(Arrays.asList(testPlayer));

        // When
        List<Player> players = playersRepository.getAllPlayers();

        // Then
        assertNotNull(players);
        assertEquals(1, players.size());
        Player player = players.get(0);
        assertEquals(TEST_PLAYER_ID, player.getPlayerId());
        assertEquals("Test Player", player.getName());
        assertEquals("test-team-id", player.getTeamId());
        assertEquals("Test Team", player.getTeamName());
        assertEquals(23, player.getJerseyNumber());
        assertEquals("SF", player.getPosition());
    }

    @Test
    void getPlayerStats_ShouldReturnStats() {
        // Given
        when(jdbcTemplate.queryForObject(
            anyString(), 
            any(RowMapper.class),
            eq(TEST_PLAYER_ID),
            eq(TEST_SEASON_ID)
        )).thenReturn(testStats);

        // When
        PlayerStats stats = playersRepository.getPlayerStats(TEST_PLAYER_ID, TEST_SEASON_ID);

        // Then
        assertNotNull(stats);
        assertEquals(TEST_PLAYER_ID, stats.getPlayerId());
        assertEquals(TEST_SEASON_ID, stats.getSeasonId());
        assertEquals(10, stats.getGames());
        assertEquals(25.5, stats.getPpg());
        assertEquals(7.2, stats.getApg());
        assertEquals(6.3, stats.getRpg());
        assertEquals(1.4, stats.getSpg());
        assertEquals(0.8, stats.getBpg());
        assertEquals(2.1, stats.getTopg());
        assertEquals(32.5, stats.getMpg());
    }

    @Test
    void getPlayerStats_WhenNoStats_ShouldReturnNull() {
        // Given
        when(jdbcTemplate.queryForObject(
            anyString(),
            any(RowMapper.class),
            eq(TEST_PLAYER_ID),
            eq(TEST_SEASON_ID)
        )).thenReturn(null);

        // When
        PlayerStats stats = playersRepository.getPlayerStats(TEST_PLAYER_ID, TEST_SEASON_ID);

        // Then
        assertNull(stats);
    }
} 