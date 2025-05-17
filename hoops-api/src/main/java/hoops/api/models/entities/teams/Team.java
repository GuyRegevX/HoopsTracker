package hoops.api.models.entities.teams;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class Team {
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
} 