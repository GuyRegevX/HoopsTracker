package hoops.api.repositories.teams;

import hoops.api.models.entities.teams.Team;
import hoops.api.models.entities.teams.TeamStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TeamsRepositoryImpl implements TeamsRepository {
    private static final Logger log = LoggerFactory.getLogger(TeamsRepositoryImpl.class);
    private final DataSource dataSource;

    @Autowired
    public TeamsRepositoryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Team> getAllTeams() {
        List<Team> teams = new ArrayList<>();
        String sql = """
            SELECT t.team_id, t.name, t.league_id, l.name as league_name, 
                   t.country, t.city, t.division, t.conference, t.arena,
                   t.founded_year, t.created_at, t.last_updated
            FROM teams t
            LEFT JOIN leagues l ON t.league_id = l.league_id
            ORDER BY t.name
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                teams.add(mapResultSetToTeam(rs));
            }
        } catch (SQLException e) {
            log.error("SQL State: {}, Error Code: {}, Message: {}", e.getSQLState(), e.getErrorCode(), e.getMessage());
            throw new RuntimeException("Error fetching teams", e);
        }
        return teams;
    }

    @Override
    public TeamStats getTeamStats(String teamId, String seasonId) {
        String sql = """
            SELECT team_id, season_id, ppg, apg, rpg,
                   spg, bpg, topg, mpg, games_played as games,
                   last_updated
            FROM team_combined_stats
            WHERE team_id = ?::uuid AND season_id = ?::uuid
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, teamId);
            ps.setString(2, seasonId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTeamStats(rs);
                }
            }
        } catch (SQLException e) {
            log.error("Error getting team stats for team {} in season {}: {}", teamId, seasonId, e.getMessage());
        }
        return null;
    }

    private Team mapResultSetToTeam(ResultSet rs) throws SQLException {
        Team team = new Team();
        team.setTeamId(rs.getString("team_id"));
        team.setName(rs.getString("name"));
        team.setLeagueId(rs.getString("league_id"));
        team.setLeagueName(rs.getString("league_name"));
        team.setCountry(rs.getString("country"));
        team.setCity(rs.getString("city"));
        team.setDivision(rs.getString("division"));
        team.setConference(rs.getString("conference"));
        team.setArena(rs.getString("arena"));
        team.setFoundedYear(rs.getInt("founded_year"));
        team.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        team.setLastUpdated(rs.getObject("last_updated", OffsetDateTime.class));
        return team;
    }

    private TeamStats mapResultSetToTeamStats(ResultSet rs) throws SQLException {
        TeamStats stats = new TeamStats();
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