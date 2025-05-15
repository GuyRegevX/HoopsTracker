package hoops.api.models.dtos;

import lombok.Data;

@Data
public class TeamStatsDTO {
    private String eventId;
    private String teamId;
    private Integer games;
    private Double ppg;
    private Double apg;
    private Double rpg;
    private Double spg;
    private Double bpg;
    private Double topg;
    private Double mpg;
} 