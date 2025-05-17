package hoops.processor.repositories.seasons;

import hoops.processor.models.entities.Seasons;
import hoops.processor.config.TestTimescaleDBConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest(classes = {SeasonRepositoryImpl.class})
@Import(TestTimescaleDBConfig.class)
@Testcontainers
@ActiveProfiles("test")
class SeasonRepositoryImplTest {

    private static final String INSERT_SEASON_SQL = """
        INSERT INTO seasons (season_id, name, start_date, end_date, active)
        VALUES (?, ?, ?, ?, ?)
        """;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SeasonRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        // Clean up the seasons table before each test
        jdbcTemplate.execute("DELETE FROM player_stat_events");
        jdbcTemplate.execute("DELETE FROM games");
        jdbcTemplate.execute("DELETE FROM seasons");
    }

    @Test
    void getActiveSession_WithDataInDB_ReturnsCorrectSeason() {
        // Arrange
        String seasonId = "1";
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update(INSERT_SEASON_SQL,
                seasonId,
                "2023-2024",
                now.minusMonths(2),  // start date
                now.plusMonths(4),   // end date
                true               // active
        );

        // Also insert an inactive season to verify filtering
        UUID inactiveSeasonId = UUID.randomUUID();
        jdbcTemplate.update(INSERT_SEASON_SQL,
                inactiveSeasonId,
                "2022-2023",
                now.minusYears(1),
                now.minusMonths(6),
                false
        );

        // Act
        Optional<Seasons> result = repository.getActiveSession();

        // Assert
        assertTrue(result.isPresent());
        Seasons activeSeason = result.get();
        assertEquals(seasonId, activeSeason.getId());
        assertEquals("2023-2024", activeSeason.getName());
        assertTrue(activeSeason.isActive());
    }

    @Test
    void getActiveSession_WithNoActiveSeasons_ReturnsEmpty() {
        // Arrange
        String seasonId = "seasonId2";

        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update(INSERT_SEASON_SQL,
                seasonId,
                "2022-2023",
                now.minusYears(1),
                now.minusMonths(6),
                false
        );

        // Act
        Optional<Seasons> result = repository.getActiveSession();

        // Assert
        assertTrue(result.isEmpty());
    }
}