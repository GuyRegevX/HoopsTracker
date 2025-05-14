package hoops.api.models.dtos.players;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class PlayerMetaDTO {
    private String playerId;
    private String name;
    private String teamId;
    private String teamName;
    private Integer jerseyNumber;
    private String position;
    private OffsetDateTime lastUpdated;
} 