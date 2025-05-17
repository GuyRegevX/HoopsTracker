package hoops.api.mappers; // Update with your actual package

import hoops.api.models.entities.players.Player;
import hoops.api.models.entities.players.PlayerStats;
import hoops.api.models.dtos.players.PlayerMetaDTO;
import hoops.api.models.dtos.players.PlayerStatsDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PlayerMapper {
    PlayerMetaDTO toPlayerMetaDTO(Player player);
    PlayerStatsDTO toPlayerStatsDTO(PlayerStats stats);
}