package hoops.api.models.dtos;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class TeamDTO {
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
    private OffsetDateTime lastUpdated;

    // Team stats
    private String eventId;
    private Integer games;
    private Double ppg;
    private Double apg;
    private Double rpg;
    private Double spg;
    private Double bpg;
    private Double topg;
    private Double mpg;
} 