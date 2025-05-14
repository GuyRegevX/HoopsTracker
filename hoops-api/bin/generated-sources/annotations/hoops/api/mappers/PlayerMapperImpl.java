package hoops.api.mappers;

import hoops.api.models.dtos.players.PlayerMetaDTO;
import hoops.api.models.dtos.players.PlayerStatsDTO;
import hoops.api.models.entities.players.Player;
import hoops.api.models.entities.players.PlayerStats;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-15T01:41:40+0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.z20250331-1358, environment: Java 21.0.6 (Eclipse Adoptium)"
)
@Component
public class PlayerMapperImpl implements PlayerMapper {

    @Override
    public PlayerMetaDTO toPlayerMetaDTO(Player player) {
        if ( player == null ) {
            return null;
        }

        PlayerMetaDTO playerMetaDTO = new PlayerMetaDTO();

        playerMetaDTO.setJerseyNumber( player.getJerseyNumber() );
        playerMetaDTO.setLastUpdated( player.getLastUpdated() );
        playerMetaDTO.setName( player.getName() );
        playerMetaDTO.setPlayerId( player.getPlayerId() );
        playerMetaDTO.setPosition( player.getPosition() );
        playerMetaDTO.setTeamId( player.getTeamId() );
        playerMetaDTO.setTeamName( player.getTeamName() );

        return playerMetaDTO;
    }

    @Override
    public PlayerStatsDTO toPlayerStatsDTO(PlayerStats playerStats) {
        if ( playerStats == null ) {
            return null;
        }

        PlayerStatsDTO playerStatsDTO = new PlayerStatsDTO();

        playerStatsDTO.setApg( playerStats.getApg() );
        playerStatsDTO.setBpg( playerStats.getBpg() );
        playerStatsDTO.setGames( playerStats.getGames() );
        playerStatsDTO.setMpg( playerStats.getMpg() );
        playerStatsDTO.setPlayerId( playerStats.getPlayerId() );
        playerStatsDTO.setPpg( playerStats.getPpg() );
        playerStatsDTO.setRpg( playerStats.getRpg() );
        playerStatsDTO.setSpg( playerStats.getSpg() );
        playerStatsDTO.setTopg( playerStats.getTopg() );

        return playerStatsDTO;
    }
}
