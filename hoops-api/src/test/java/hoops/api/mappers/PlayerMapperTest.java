package hoops.api.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import hoops.api.models.dtos.players.PlayerMetaDTO;
import hoops.api.models.dtos.players.PlayerStatsDTO;
import hoops.api.models.entities.players.Player;
import hoops.api.models.entities.players.PlayerStats;

class PlayerMapperTest {
    private final PlayerMapper mapper = Mappers.getMapper(PlayerMapper.class);

    @Test
    void shouldMapPlayerToPlayerMetaDTO() {
        // Given
        Player player = new Player();
        player.setPlayerId("player123");
        player.setName("LeBron James");
        player.setTeamId("lakers");
        player.setTeamName("Los Angeles Lakers");
        player.setJerseyNumber(23);
        player.setPosition("F");
        player.setCreatedAt(OffsetDateTime.now());
        player.setLastUpdated(OffsetDateTime.now());

        // When
        PlayerMetaDTO dto = mapper.toPlayerMetaDTO(player);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getPlayerId()).isEqualTo(player.getPlayerId());
        assertThat(dto.getName()).isEqualTo(player.getName());
        assertThat(dto.getTeamId()).isEqualTo(player.getTeamId());
        assertThat(dto.getTeamName()).isEqualTo(player.getTeamName());
        assertThat(dto.getJerseyNumber()).isEqualTo(player.getJerseyNumber());
        assertThat(dto.getPosition()).isEqualTo(player.getPosition());
        assertThat(dto.getLastUpdated()).isNotNull();
    }

    @Test
    void shouldMapPlayerStatsToPlayerStatsDTO() {
        // Given
        PlayerStats stats = new PlayerStats();
        stats.setPlayerId("player123");
        stats.setTeamId("lakers");
        stats.setSeasonId("2023");
        stats.setGames(82);
        stats.setPpg(27.4);
        stats.setRpg(7.9);
        stats.setApg(8.3);
        stats.setSpg(1.2);
        stats.setBpg(0.8);
        stats.setTopg(3.1);
        stats.setMpg(36.5);
        stats.setLastUpdated(OffsetDateTime.now());

        // When
        PlayerStatsDTO dto = mapper.toPlayerStatsDTO(stats);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getPlayerId()).isEqualTo(stats.getPlayerId());
        assertThat(dto.getTeamId()).isEqualTo(stats.getTeamId());
        assertThat(dto.getSeasonId()).isEqualTo(stats.getSeasonId());
        assertThat(dto.getGames()).isEqualTo(stats.getGames());
        assertThat(dto.getPpg()).isEqualTo(stats.getPpg());
        assertThat(dto.getRpg()).isEqualTo(stats.getRpg());
        assertThat(dto.getApg()).isEqualTo(stats.getApg());
        assertThat(dto.getSpg()).isEqualTo(stats.getSpg());
        assertThat(dto.getBpg()).isEqualTo(stats.getBpg());
        assertThat(dto.getTopg()).isEqualTo(stats.getTopg());
        assertThat(dto.getMpg()).isEqualTo(stats.getMpg());
    }

    @Test
    void shouldHandleNullInputs() {
        // When
        PlayerMetaDTO metaDto = mapper.toPlayerMetaDTO(null);
        PlayerStatsDTO statsDto = mapper.toPlayerStatsDTO(null);

        // Then
        assertThat(metaDto).isNull();
        assertThat(statsDto).isNull();
    }
}