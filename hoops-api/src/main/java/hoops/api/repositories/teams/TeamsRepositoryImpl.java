package hoops.api.repositories.teams; // Update with your actual package name

import hoops.api.models.entities.teams.Team;
import hoops.api.models.entities.teams.TeamStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
        SELECT 
            team_id, 
            season_id,
            games,
            ppg, 
            apg, 
            rpg,
            spg, 
            bpg, 
            topg,
            mpg,
            last_updated
        FROM team_avg_stats_view
        WHERE team_id = ?
        AND season_id = ?
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, teamId);
            ps.setString(2, seasonId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTeamStatsFromView(rs);
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

    // Updated mapper for team stats from the materialized view
    private TeamStats mapResultSetToTeamStatsFromView(ResultSet rs) throws SQLException {
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

        // Set mpg to a default value since it's not in the view
        stats.setMpg(0.0);  // Alternatively, you could calculate it if you have the data

        stats.setLastUpdated(rs.getObject("last_updated", OffsetDateTime.class));
        return stats;
    }
}