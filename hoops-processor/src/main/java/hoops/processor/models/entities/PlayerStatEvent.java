package hoops.processor.models.entities;

import hoops.common.enums.StatType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class PlayerStatEvent {
    private Integer eventId;   // SERIAL - auto-incremented by database
    private String playerId;   // TEXT NOT NULL
    private String gameId;     // SERIAL NOT NULL
    private String teamId;     // SERIAL NOT NULL
    private String seasonId;   // SERIAL NOT NULL (added this field)
    private StatType statType; // TEXT NOT NULL
    private Double statValue;  // INTEGER NOT NULL
    private long version;      // INTEGER NOT NULL DEFAULT 1
}
