package hoops.api.models.entities;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class Team {
    // Team metadata
    private String teamId;
    private String name;
    private String leagueId;
    private String leagueName;
    private String country;
    private String city;
    private String division;
    private String conference;
    private String arena;
    private Integer foundedYear;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastUpdated;

    // Team stats
    private String gameId;
    private String seasonId;
    private Integer games;
    private Double ppg;  // points per game
    private Double apg;  // assists per game
    private Double rpg;  // rebounds per game
    private Double spg;  // steals per game
    private Double bpg;  // blocks per game
    private Double topg; // turnovers per game
    private Double mpg;  // minutes per game
} 