
package hoops.api.mappers;

import hoops.api.models.dtos.teams.TeamMetaDTO;
import hoops.api.models.dtos.teams.TeamStatsDTO;
import hoops.api.models.entities.teams.Team;
import hoops.api.models.entities.teams.TeamStats;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TeamMapperTest {
    private final TeamMapper mapper = Mappers.getMapper(TeamMapper.class);

    @Test
    void shouldMapTeamToTeamMetaDTO() {
        // Given
        Team team = new Team();
        team.setTeamId("team123");
        team.setName("Los Angeles Lakers");
        team.setLeagueId("nba");
        team.setLeagueName("NBA");
        team.setCountry("USA");
        team.setCity("Los Angeles");
        team.setDivision("Pacific");
        team.setConference("Western");
        team.setArena("Crypto.com Arena");
        team.setFoundedYear(1947);
        team.setCreatedAt(OffsetDateTime.now());
        team.setLastUpdated(OffsetDateTime.now());

        // When
        TeamMetaDTO dto = mapper.toTeamMetaDTO(team);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getTeamId()).isEqualTo(team.getTeamId());
        assertThat(dto.getName()).isEqualTo(team.getName());
        assertThat(dto.getLeagueId()).isEqualTo(team.getLeagueId());
        assertThat(dto.getLeagueName()).isEqualTo(team.getLeagueName());
        assertThat(dto.getCountry()).isEqualTo(team.getCountry());
        assertThat(dto.getLastUpdated()).isNotNull();
    }

    @Test
    void shouldMapTeamStatsToTeamStatsDTO_WithTwoDecimalPlaces() {
        // Given
        TeamStats stats = new TeamStats();
        stats.setTeamId("team123");
        stats.setSeasonId("2023");
        stats.setGames(82);
        stats.setPpg(114.5);
        stats.setApg(25.8);
        stats.setRpg(44.2);
        stats.setSpg(7.5);
        stats.setBpg(5.2);
        stats.setTopg(13.1);
        stats.setMpg(240.0);
        stats.setLastUpdated(OffsetDateTime.now());

        // When
        TeamStatsDTO dto = mapper.toTeamStatsDTO(stats);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getTeamId()).isEqualTo(stats.getTeamId());
        assertThat(dto.getSeasonId()).isEqualTo(stats.getSeasonId());
        assertThat(dto.getGames()).isEqualTo(stats.getGames());

        // Check formatting with exactly two decimal places
        assertThat(dto.getPpg()).isEqualTo(114.50);
        assertThat(dto.getApg()).isEqualTo(25.80);
        assertThat(dto.getRpg()).isEqualTo(44.20);
        assertThat(dto.getSpg()).isEqualTo(7.50);
        assertThat(dto.getBpg()).isEqualTo(5.20);
        assertThat(dto.getTopg()).isEqualTo(13.10);
        assertThat(dto.getMpg()).isEqualTo(240.00);

        // Check string representation to verify formatting
        assertThat(String.format("%.2f", dto.getPpg())).isEqualTo("114.50");
        assertThat(String.format("%.2f", dto.getRpg())).isEqualTo("44.20");
        assertThat(String.format("%.2f", dto.getApg())).isEqualTo("25.80");
    }

    @Test
    void shouldHandleNullValues_WithDefaultZeroDecimals() {
        // Given
        TeamStats stats = new TeamStats();
        stats.setTeamId("team123");
        stats.setSeasonId("2023");
        stats.setGames(82);

        // When
        TeamStatsDTO dto = mapper.toTeamStatsDTO(stats);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getPpg()).isEqualTo(0.00);
        assertThat(dto.getRpg()).isEqualTo(0.00);

        // Check string representation for proper formatting
        assertThat(String.format("%.2f", dto.getPpg())).isEqualTo("0.00");
    }

    @Test
    void shouldHandleNullInputs() {
        // When
        TeamMetaDTO metaDto = mapper.toTeamMetaDTO(null);
        TeamStatsDTO statsDto = mapper.toTeamStatsDTO(null);

        // Then
        assertThat(metaDto).isNull();
        assertThat(statsDto).isNull();
    }

    @Test
    void shouldFormatMaximumValuesWithExactlyTwoDecimals() {
        // Given - stats with maximum or varied decimal places
        TeamStats stats = new TeamStats();
        stats.setTeamId("team_max");
        stats.setSeasonId("2023");
        stats.setGames(82);

        // Set values with different decimal precisions
        stats.setPpg(120.0);      // Whole number
        stats.setRpg(45.1);       // One decimal
        stats.setApg(27.23);      // Two decimals
        stats.setSpg(8.456);      // Three decimals
        stats.setBpg(6.5678);     // Four decimals
        stats.setTopg(12.00);     // Zero with decimal
        stats.setMpg(240);        // Integer

        // When
        TeamStatsDTO dto = mapper.toTeamStatsDTO(stats);

        // Then
        assertThat(dto).isNotNull();

        // All values should be formatted with exactly two decimal places
        assertThat(dto.getPpg()).isEqualTo(120.00);
        assertThat(dto.getRpg()).isEqualTo(45.10);
        assertThat(dto.getApg()).isEqualTo(27.23);
        assertThat(dto.getSpg()).isEqualTo(8.46);  // Rounded to 2 places
        assertThat(dto.getBpg()).isEqualTo(6.57);  // Rounded to 2 places
        assertThat(dto.getTopg()).isEqualTo(12.00);
        assertThat(dto.getMpg()).isEqualTo(240.00);

        // Verify decimal string representation
        assertThat(String.format("%.2f", dto.getPpg())).isEqualTo("120.00");
        assertThat(String.format("%.2f", dto.getRpg())).isEqualTo("45.10");
        assertThat(String.format("%.2f", dto.getApg())).isEqualTo("27.23");
        assertThat(String.format("%.2f", dto.getSpg())).isEqualTo("8.46");
        assertThat(String.format("%.2f", dto.getBpg())).isEqualTo("6.57");
        assertThat(String.format("%.2f", dto.getTopg())).isEqualTo("12.00");
        assertThat(String.format("%.2f", dto.getMpg())).isEqualTo("240.00");
    }
}