package hoops.processor.repositories.PlayerStatEvents;

import hoops.processor.models.entities.PlayerStatEvent;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;


@AllArgsConstructor
@Repository
public class PlayerStatEventsRepositoryImpl implements PlayerStatEventsRepository{

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void save(PlayerStatEvent playerStatEvent) {
        String sql = """
            INSERT INTO player_stat_events (
                player_id, game_id, team_id, season_id,
                stat_type, stat_value, version
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
            RETURNING event_id, version
            """;
    
        try {
            // Use queryForMap to get result as a Map, which is more reliable
            Map<String, Object> result = jdbcTemplate.queryForMap(
                    sql,
                    playerStatEvent.getPlayerId(),
                    playerStatEvent.getGameId(),
                    playerStatEvent.getTeamId(),
                    playerStatEvent.getSeasonId(),
                    playerStatEvent.getStatType().getValue(),
                    playerStatEvent.getStatValue(),
                    playerStatEvent.getVersion()
            );
            
            // Set the event ID from the returned value
            playerStatEvent.setEventId((Integer) result.get("event_id"));
            
            // Verify version is the same as what we sent
            long returnedVersion = ((Number) result.get("version")).longValue();
            if (returnedVersion != playerStatEvent.getVersion()) {
                playerStatEvent.setVersion(returnedVersion);
            }
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to save player stat event", e);
        }
    }
}
