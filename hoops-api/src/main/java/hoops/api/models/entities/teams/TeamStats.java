package hoops.api.models.entities.teams;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class TeamStats {
    private String teamId;
    private String seasonId;
    private Integer games;
    private Double ppg;  // points per game
    private Double apg;  // assists per game
    private Double rpg;  // rebounds per game
    private Double spg;  // steals per game
    private Double bpg;  // blocks per game
    private Double topg; // turnovers per game
    private Double mpg;  // minutes per game
    private OffsetDateTime lastUpdated;
} 