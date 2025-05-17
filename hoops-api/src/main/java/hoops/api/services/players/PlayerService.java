package hoops.api.services.players;

import hoops.api.models.dtos.players.PlayerMetaDTO;
import hoops.api.models.dtos.players.PlayerStatsDTO;
import java.util.List;

/**
 * Service interface for managing player-related operations
 */
public interface PlayerService {
    /**
     * Get all players with their metadata
     * @return List of player DTOs with metadata
     */
    List<PlayerMetaDTO> getAllPlayers();

    /**
     * Get player statistics for a specific player and season
     * @param playerId The player ID
     * @param seasonId The season ID for time-series data
     * @return Player statistics DTO
     */
    PlayerStatsDTO getPlayerStats(String playerId, String seasonId);
} 