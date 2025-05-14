package hoops.api.repositories.teams;

import hoops.api.models.entities.teams.Team;
import hoops.api.models.entities.teams.TeamStats;
import java.util.List;

public interface TeamsRepository {
    /**
     * Get all teams from the database
     * @return List of teams with metadata information
     */
    List<Team> getAllTeams();

    /**
     * Get team statistics for a specific team and season
     * @param teamId The team ID
     * @param seasonId The season ID
     * @return Team with statistics or null if not found
     */
    TeamStats getTeamStats(String teamId, String seasonId);
} 