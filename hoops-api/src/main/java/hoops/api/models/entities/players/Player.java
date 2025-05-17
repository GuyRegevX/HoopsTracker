package hoops.api.models.entities.players;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class Player {
    private String playerId;
    private String name;
    private String teamId;
    private String teamName;
    private Integer jerseyNumber;
    private String position;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastUpdated;
} 