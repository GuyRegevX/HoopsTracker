package hoops.api.services;

import hoops.api.models.dtos.TeamMetaDTO;
import hoops.api.models.dtos.TeamStatsDTO;
import java.util.List;

/**
 * Service interface for managing team-related operations
 */
public interface TeamsService {
    /**
     * Get all teams with their metadata
     * @return List of team DTOs with metadata
     */
    List<TeamMetaDTO> getAllTeams();

    /**
     * Get team statistics for a specific team and season
     * @param teamId The team ID
     * @param seasonId The season ID for time-series data
     * @return Team statistics DTO
     */
    TeamStatsDTO getTeamStats(String teamId, String seasonId);
} 