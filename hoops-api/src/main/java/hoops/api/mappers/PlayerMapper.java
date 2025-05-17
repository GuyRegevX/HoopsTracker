package hoops.api.mappers;

import hoops.api.models.dtos.players.PlayerMetaDTO;
import hoops.api.models.dtos.players.PlayerStatsDTO;
import hoops.api.models.entities.players.Player;
import hoops.api.models.entities.players.PlayerStats;
import org.springframework.stereotype.Component;

@Component
public class PlayerMapper {
    
    public PlayerMetaDTO toPlayerMetaDTO(Player player) {
        if (player == null) return null;
        
        PlayerMetaDTO dto = new PlayerMetaDTO();
        dto.setPlayerId(player.getPlayerId());
        dto.setName(player.getName());
        dto.setTeamId(player.getTeamId());
        dto.setTeamName(player.getTeamName());
        dto.setJerseyNumber(player.getJerseyNumber());
        dto.setPosition(player.getPosition());
        dto.setLastUpdated(player.getLastUpdated());
        return dto;
    }

    public PlayerStatsDTO toPlayerStatsDTO(PlayerStats stats) {
        if (stats == null) return null;
        
        PlayerStatsDTO dto = new PlayerStatsDTO();
        dto.setPlayerId(stats.getPlayerId());
        dto.setGames(stats.getGames());
        dto.setPpg(stats.getPpg());
        dto.setApg(stats.getApg());
        dto.setRpg(stats.getRpg());
        dto.setSpg(stats.getSpg());
        dto.setBpg(stats.getBpg());
        dto.setTopg(stats.getTopg());
        dto.setMpg(stats.getMpg());
        return dto;
    }
} 