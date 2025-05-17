package hoops.processor.repositories.seasons;

import hoops.processor.models.entities.Seasons;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Repository
public class SeasonRepositoryImpl implements SeasonRepository {
    
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<Seasons> getActiveSession() {
        String sql = "SELECT * FROM seasons WHERE active = true";
        List<Seasons> results = jdbcTemplate.query(sql, (rs, rowNum) -> Seasons.builder()
                .id(rs.getString("season_id"))
                .name(rs.getString("name"))
                .isActive(rs.getBoolean("active"))
                .build());
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}