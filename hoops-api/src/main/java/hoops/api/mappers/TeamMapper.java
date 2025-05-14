package hoops.api.mappers;

import hoops.api.models.dtos.teams.TeamDTO;
import hoops.api.models.dtos.teams.TeamMetaDTO;
import hoops.api.models.dtos.teams.TeamStatsDTO;
import hoops.api.models.entities.teams.Team;
import hoops.api.models.entities.teams.TeamStats;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface TeamMapper {
    TeamMetaDTO toTeamMetaDTO(Team team);
    TeamStatsDTO toTeamStatsDTO(TeamStats teamStats);
    
    @Mapping(source = "team.teamId", target = "teamId")
    @Mapping(source = "team.lastUpdated", target = "lastUpdated")
    TeamDTO toTeamDTO(Team team, TeamStats stats);
} 