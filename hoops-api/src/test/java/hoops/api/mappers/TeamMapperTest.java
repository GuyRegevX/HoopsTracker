package hoops.api.mappers;

import hoops.api.models.dtos.teams.TeamDTO;
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
    void shouldMapTeamToTeamStatsDTO() {
        // Given
        TeamStats stats = new TeamStats();
        stats.setTeamId("team123");
        stats.setGames(82);
        stats.setPpg(114.5);
        stats.setApg(25.8);
        stats.setRpg(44.2);
        stats.setSpg(7.5);
        stats.setBpg(5.2);
        stats.setTopg(13.1);
        stats.setMpg(240.0);

        // When
        TeamStatsDTO dto = mapper.toTeamStatsDTO(stats);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getTeamId()).isEqualTo(stats.getTeamId());
        assertThat(dto.getGames()).isEqualTo(stats.getGames());
        assertThat(dto.getPpg()).isEqualTo(stats.getPpg());
        assertThat(dto.getApg()).isEqualTo(stats.getApg());
        assertThat(dto.getRpg()).isEqualTo(stats.getRpg());
        assertThat(dto.getSpg()).isEqualTo(stats.getSpg());
        assertThat(dto.getBpg()).isEqualTo(stats.getBpg());
        assertThat(dto.getTopg()).isEqualTo(stats.getTopg());
        assertThat(dto.getMpg()).isEqualTo(stats.getMpg());
    }
} 