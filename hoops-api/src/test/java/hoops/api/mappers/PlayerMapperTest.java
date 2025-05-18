
package hoops.api.mappers;

import hoops.api.models.dtos.players.PlayerMetaDTO;
import hoops.api.models.dtos.players.PlayerStatsDTO;
import hoops.api.models.entities.players.Player;
import hoops.api.models.entities.players.PlayerStats;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

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
    void shouldMapPlayerStatsToPlayerStatsDTO_WithTwoDecimalPlaces() {
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

        // Check formatting with exactly two decimal places
        assertThat(dto.getPpg()).isEqualTo(27.40);
        assertThat(dto.getRpg()).isEqualTo(7.90);
        assertThat(dto.getApg()).isEqualTo(8.30);
        assertThat(dto.getSpg()).isEqualTo(1.20);
        assertThat(dto.getBpg()).isEqualTo(0.80);
        assertThat(dto.getTopg()).isEqualTo(3.10);
        assertThat(dto.getMpg()).isEqualTo(36.50);

        // Check string representation to verify formatting
        assertThat(dto.getPpg().toString()).endsWith(".4");
        assertThat(dto.getRpg().toString()).endsWith(".9");
        assertThat(dto.getApg().toString()).endsWith(".3");
    }

    @Test
    void shouldHandleNullValues_WithDefaultZeroDecimals() {
        // Given
        PlayerStats stats = new PlayerStats();
        stats.setPlayerId("player123");
        stats.setTeamId("lakers");
        stats.setSeasonId("2023");
        stats.setGames(82);
        // Leave some values as null
        stats.setPpg(null);
        stats.setRpg(null);

        // When
        PlayerStatsDTO dto = mapper.toPlayerStatsDTO(stats);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getPpg()).isEqualTo(0.00);
        assertThat(dto.getRpg()).isEqualTo(0.00);

        // Check string representation for proper formatting
        assertThat(dto.getPpg().toString()).endsWith(".0");
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

    @Test
    void shouldFormatMaximumValuesWithExactlyTwoDecimals() {
        // Given - stats with maximum or varied decimal places
        PlayerStats stats = new PlayerStats();
        stats.setPlayerId("player_max");
        stats.setTeamId("teamId");
        stats.setSeasonId("2023");
        stats.setGames(82);

        // Set values with different decimal precisions
        stats.setPpg(42.0);       // Whole number
        stats.setRpg(15.1);       // One decimal
        stats.setApg(11.23);      // Two decimals
        stats.setSpg(2.456);      // Three decimals
        stats.setBpg(3.5678);     // Four decimals
        stats.setTopg(1.00);      // Zero with decimal
        stats.setMpg(38.0);         // Integer

        // When
        PlayerStatsDTO dto = mapper.toPlayerStatsDTO(stats);

        // Then
        assertThat(dto).isNotNull();

        // All values should be formatted with exactly two decimal places
        assertThat(dto.getPpg()).isEqualTo(42.00);
        assertThat(dto.getRpg()).isEqualTo(15.10);
        assertThat(dto.getApg()).isEqualTo(11.23);
        assertThat(dto.getSpg()).isEqualTo(2.46);  // Rounded to 2 places
        assertThat(dto.getBpg()).isEqualTo(3.57);  // Rounded to 2 places
        assertThat(dto.getTopg()).isEqualTo(1.00);
        assertThat(dto.getMpg()).isEqualTo(38.00);

        // Verify decimal string representation
        assertThat(String.format("%.2f", dto.getPpg())).isEqualTo("42.00");
        assertThat(String.format("%.2f", dto.getRpg())).isEqualTo("15.10");
        assertThat(String.format("%.2f", dto.getApg())).isEqualTo("11.23");
        assertThat(String.format("%.2f", dto.getSpg())).isEqualTo("2.46");
        assertThat(String.format("%.2f", dto.getBpg())).isEqualTo("3.57");
        assertThat(String.format("%.2f", dto.getTopg())).isEqualTo("1.00");
        assertThat(String.format("%.2f", dto.getMpg())).isEqualTo("38.00");
    }
}