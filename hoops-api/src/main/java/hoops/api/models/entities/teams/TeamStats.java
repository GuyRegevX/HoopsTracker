package hoops.api.models.entities.teams;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class TeamStats {
    private String teamId;
    private String seasonId;
    private String gameId; // For live games

    // Team Metadata
    private String teamName;
    private String leagueId;
    private String leagueName;

    // Stats
    private int games;
    private double ppg;
    private double apg;
    private double rpg;
    private double spg;
    private double bpg;
    private double topg;
    private double mpg;

    // Timestamps
    private OffsetDateTime time;
    private OffsetDateTime lastUpdated;
} 