package hoops.api.mappers;

import hoops.api.models.dtos.teams.TeamDTO;
import hoops.api.models.dtos.teams.TeamMetaDTO;
import hoops.api.models.dtos.teams.TeamStatsDTO;
import hoops.api.models.entities.teams.Team;
import hoops.api.models.entities.teams.TeamStats;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-15T01:41:40+0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.z20250331-1358, environment: Java 21.0.6 (Eclipse Adoptium)"
)
@Component
public class TeamMapperImpl implements TeamMapper {

    @Override
    public TeamMetaDTO toTeamMetaDTO(Team team) {
        if ( team == null ) {
            return null;
        }

        TeamMetaDTO teamMetaDTO = new TeamMetaDTO();

        teamMetaDTO.setCountry( team.getCountry() );
        teamMetaDTO.setLastUpdated( team.getLastUpdated() );
        teamMetaDTO.setLeagueId( team.getLeagueId() );
        teamMetaDTO.setLeagueName( team.getLeagueName() );
        teamMetaDTO.setName( team.getName() );
        teamMetaDTO.setTeamId( team.getTeamId() );

        return teamMetaDTO;
    }

    @Override
    public TeamStatsDTO toTeamStatsDTO(TeamStats teamStats) {
        if ( teamStats == null ) {
            return null;
        }

        TeamStatsDTO teamStatsDTO = new TeamStatsDTO();

        teamStatsDTO.setApg( teamStats.getApg() );
        teamStatsDTO.setBpg( teamStats.getBpg() );
        teamStatsDTO.setGames( teamStats.getGames() );
        teamStatsDTO.setMpg( teamStats.getMpg() );
        teamStatsDTO.setPpg( teamStats.getPpg() );
        teamStatsDTO.setRpg( teamStats.getRpg() );
        teamStatsDTO.setSpg( teamStats.getSpg() );
        teamStatsDTO.setTeamId( teamStats.getTeamId() );
        teamStatsDTO.setTopg( teamStats.getTopg() );

        return teamStatsDTO;
    }

    @Override
    public TeamDTO toTeamDTO(Team team, TeamStats stats) {
        if ( team == null && stats == null ) {
            return null;
        }

        TeamDTO teamDTO = new TeamDTO();

        if ( team != null ) {
            teamDTO.setTeamId( team.getTeamId() );
            teamDTO.setLastUpdated( team.getLastUpdated() );
            teamDTO.setArena( team.getArena() );
            teamDTO.setCity( team.getCity() );
            teamDTO.setConference( team.getConference() );
            teamDTO.setCountry( team.getCountry() );
            teamDTO.setDivision( team.getDivision() );
            teamDTO.setFoundedYear( team.getFoundedYear() );
            teamDTO.setLeagueId( team.getLeagueId() );
            teamDTO.setLeagueName( team.getLeagueName() );
            teamDTO.setName( team.getName() );
        }
        if ( stats != null ) {
            teamDTO.setApg( stats.getApg() );
            teamDTO.setBpg( stats.getBpg() );
            teamDTO.setGames( stats.getGames() );
            teamDTO.setMpg( stats.getMpg() );
            teamDTO.setPpg( stats.getPpg() );
            teamDTO.setRpg( stats.getRpg() );
            teamDTO.setSpg( stats.getSpg() );
            teamDTO.setTopg( stats.getTopg() );
        }

        return teamDTO;
    }
}
