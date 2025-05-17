package hoops.api.repositories.players; // Update with your actual package name

import hoops.api.models.entities.players.Player;
import hoops.api.models.entities.players.PlayerStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class PlayerRepositoryImpl implements PlayerRepository {
    private static final Logger log = LoggerFactory.getLogger(PlayerRepositoryImpl.class);
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Player> playerRowMapper = (rs, rowNum) -> mapResultSetToPlayer(rs);
    private final RowMapper<PlayerStats> statsRowMapper = (rs, rowNum) -> mapResultSetToPlayerStats(rs);

    @Autowired
    public PlayerRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Player> getAllPlayers() {
        String sql = """
            SELECT p.player_id, p.name, p.team_id, t.name as team_name,
                   p.jersey_number, p.position,
                   p.created_at, p.last_updated
            FROM players p
            LEFT JOIN teams t ON p.team_id = t.team_id
            ORDER BY p.name
            """;

        try {
            return jdbcTemplate.query(sql, playerRowMapper);
        } catch (Exception e) {
            log.error("Error fetching players: {}", e.getMessage());
            throw new RuntimeException("Error fetching players", e);
        }
    }

    @Override
    public PlayerStats getPlayerStats(String playerId, String seasonId) {
        // Updated SQL to use player_avg_stats_view materialized view
        String sql = """
            SELECT player_id, team_id, season_id,
                   MAX(games) as games,
                   MAX(ppg) as ppg, 
                   MAX(apg) as apg, 
                   MAX(rpg) as rpg,
                   MAX(spg) as spg, 
                   MAX(bpg) as bpg, 
                   MAX(topg) as topg,
                   MAX(mpg) as mpg,
                   MAX(bucket_time) as bucket_time,
                   MAX(last_updated) as last_updated
            FROM player_avg_stats_view
            WHERE player_id = ?
            AND season_id = ?
            GROUP BY player_id, team_id, season_id
            """;

        try {
            List<PlayerStats> stats = jdbcTemplate.query(sql, statsRowMapper, playerId, seasonId);
            return stats.isEmpty() ? null : stats.get(0);
        } catch (Exception e) {
            log.error("Error getting player stats for player {} in season {}: {}", playerId, seasonId, e.getMessage());
            return null;
        }
    }

    private Player mapResultSetToPlayer(ResultSet rs) throws SQLException {
        Player player = new Player();
        player.setPlayerId(rs.getString("player_id"));
        player.setName(rs.getString("name"));
        player.setTeamId(rs.getString("team_id"));
        player.setTeamName(rs.getString("team_name"));
        player.setJerseyNumber(rs.getInt("jersey_number"));
        player.setPosition(rs.getString("position"));
        player.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        player.setLastUpdated(rs.getObject("last_updated", OffsetDateTime.class));
        return player;
    }

    private PlayerStats mapResultSetToPlayerStats(ResultSet rs) throws SQLException {
        PlayerStats stats = new PlayerStats();
        stats.setPlayerId(rs.getString("player_id"));
        stats.setTeamId(rs.getString("team_id"));
        stats.setSeasonId(rs.getString("season_id"));
        stats.setGames(rs.getInt("games"));
        stats.setPpg(rs.getDouble("ppg"));
        stats.setApg(rs.getDouble("apg"));
        stats.setRpg(rs.getDouble("rpg"));
        stats.setSpg(rs.getDouble("spg"));
        stats.setBpg(rs.getDouble("bpg"));
        stats.setTopg(rs.getDouble("topg"));
        stats.setMpg(rs.getDouble("mpg"));
        stats.setLastUpdated(rs.getObject("last_updated", OffsetDateTime.class));
        return stats;
    }
}