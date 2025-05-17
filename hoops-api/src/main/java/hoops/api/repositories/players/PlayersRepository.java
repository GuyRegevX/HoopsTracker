package hoops.api.repositories.players;

import hoops.api.models.entities.players.Player;
import hoops.api.models.entities.players.PlayerStats;
import java.util.List;

public interface PlayersRepository {
    /**
     * Get all players from the database
     * @return List of players with metadata information
     */
    List<Player> getAllPlayers();

    /**
     * Get player statistics for a specific player and season
     * @param playerId The player ID
     * @param seasonId The season ID
     * @return Player with statistics or null if not found
     */
    PlayerStats getPlayerStats(String playerId, String seasonId);
} 