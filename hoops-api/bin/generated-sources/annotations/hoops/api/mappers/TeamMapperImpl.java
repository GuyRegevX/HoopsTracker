package hoops.api.mappers;

import hoops.api.models.dtos.TeamDTO;
import hoops.api.models.dtos.TeamMetaDTO;
import hoops.api.models.dtos.TeamStatsDTO;
import hoops.api.models.entities.Team;
import hoops.api.models.entities.TeamStats;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-14T20:26:26+0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.z20250331-1358, environment: Java 21.0.6 (Eclipse Adoptium)"
)
@Component
public class TeamMapperImpl implements TeamMapper {

    @Override
    public TeamDTO toDTO(Team team) {
        if ( team == null ) {
            return null;
        }

        TeamDTO teamDTO = new TeamDTO();

        teamDTO.setApg( team.getApg() );
        teamDTO.setArena( team.getArena() );
        teamDTO.setBpg( team.getBpg() );
        teamDTO.setCity( team.getCity() );
        teamDTO.setConference( team.getConference() );
        teamDTO.setCountry( team.getCountry() );
        teamDTO.setDivision( team.getDivision() );
        teamDTO.setFoundedYear( team.getFoundedYear() );
        teamDTO.setGames( team.getGames() );
        teamDTO.setLastUpdated( team.getLastUpdated() );
        teamDTO.setLeagueId( team.getLeagueId() );
        teamDTO.setLeagueName( team.getLeagueName() );
        teamDTO.setMpg( team.getMpg() );
        teamDTO.setName( team.getName() );
        teamDTO.setPpg( team.getPpg() );
        teamDTO.setRpg( team.getRpg() );
        teamDTO.setSpg( team.getSpg() );
        teamDTO.setTeamId( team.getTeamId() );
        teamDTO.setTopg( team.getTopg() );

        return teamDTO;
    }

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
    public TeamStatsDTO toTeamStatsDTO(TeamStats stats) {
        if ( stats == null ) {
            return null;
        }

        TeamStatsDTO teamStatsDTO = new TeamStatsDTO();

        teamStatsDTO.setApg( stats.getApg() );
        teamStatsDTO.setBpg( stats.getBpg() );
        teamStatsDTO.setGames( stats.getGames() );
        teamStatsDTO.setMpg( stats.getMpg() );
        teamStatsDTO.setPpg( stats.getPpg() );
        teamStatsDTO.setRpg( stats.getRpg() );
        teamStatsDTO.setSpg( stats.getSpg() );
        teamStatsDTO.setTeamId( stats.getTeamId() );
        teamStatsDTO.setTopg( stats.getTopg() );

        return teamStatsDTO;
    }
}
