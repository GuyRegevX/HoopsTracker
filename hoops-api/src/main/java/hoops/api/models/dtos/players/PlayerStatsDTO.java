package hoops.api.models.dtos.players;

import lombok.Data;

@Data
public class PlayerStatsDTO {
    private String playerId;
    private String seasonId;
    private String teamId;
    private Integer games;
    private Double ppg;  // points per game
    private Double apg;  // assists per game
    private Double rpg;  // rebounds per game
    private Double spg;  // steals per game
    private Double bpg;  // blocks per game
    private Double topg; // turnovers per game
    private Double mpg;  // minutes per
} 