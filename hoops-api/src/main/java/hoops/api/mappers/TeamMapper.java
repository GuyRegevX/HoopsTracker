package hoops.api.mappers;

import hoops.api.models.dtos.teams.TeamMetaDTO;
import hoops.api.models.dtos.teams.TeamStatsDTO;
import hoops.api.models.entities.teams.Team;
import hoops.api.models.entities.teams.TeamStats;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface TeamMapper {
    TeamMetaDTO toTeamMetaDTO(Team team);

    @Mapping(source = "ppg", target = "ppg", qualifiedByName = "formatDouble")
    @Mapping(source = "apg", target = "apg", qualifiedByName = "formatDouble")
    @Mapping(source = "rpg", target = "rpg", qualifiedByName = "formatDouble")
    @Mapping(source = "spg", target = "spg", qualifiedByName = "formatDouble")
    @Mapping(source = "bpg", target = "bpg", qualifiedByName = "formatDouble")
    @Mapping(source = "topg", target = "topg", qualifiedByName = "formatDouble")
    @Mapping(source = "mpg", target = "mpg", qualifiedByName = "formatDouble")
    TeamStatsDTO toTeamStatsDTO(TeamStats teamStats);

    @Named("formatDouble")
    default Double formatDouble(Double value) {
        if (value == null) {
            return 0.00;
        }
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}