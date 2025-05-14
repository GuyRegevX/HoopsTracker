package hoops.api.mappers;

import hoops.api.models.dtos.TeamDTO;
import hoops.api.models.dtos.TeamMetaDTO;
import hoops.api.models.dtos.TeamStatsDTO;
import hoops.api.models.entities.Team;
import hoops.api.models.entities.TeamStats;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface TeamMapper {
    TeamDTO toDTO(Team team);
    TeamMetaDTO toTeamMetaDTO(Team team);
    TeamStatsDTO toTeamStatsDTO(TeamStats stats);
} 