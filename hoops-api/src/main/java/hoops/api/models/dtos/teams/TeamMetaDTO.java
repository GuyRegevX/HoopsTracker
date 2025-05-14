package hoops.api.models.dtos.teams;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class TeamMetaDTO {
    private String teamId;
    private String name;
    private String leagueId;
    private String leagueName;
    private String country;
    private OffsetDateTime lastUpdated;
} 