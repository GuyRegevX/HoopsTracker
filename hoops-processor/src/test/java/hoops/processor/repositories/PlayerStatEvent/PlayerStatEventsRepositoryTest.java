package hoops.processor.repositories.PlayerStatEvent;

import hoops.processor.config.TestDatabaseConfig;
import hoops.processor.models.entities.PlayerStatEvents;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@Import({TestDatabaseConfig.class, PlayerStatEventsRepositoryImpl.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class PlayerStatEventsRepositoryTest {

    @Autowired
    private PlayerStatEventsRepository repository;

    @Test
    void save_Success() {
        // Arrange
        PlayerStatEvents event = new PlayerStatEvents();
        event.setPlayerId(1L);
        event.setGameId(2L);
        event.setTeamId(3L);
        event.setSeasonId(1L); // Using the active season from init.sql
        event.setEventType("points_scored");
        event.setValue(2);
        event.setEventTime(Instant.now());
        event.setProcessedAt(Instant.now());
        event.setPlayerName("Test Player");

        // Act & Assert
        assertDoesNotThrow(() -> repository.save(event));
    }

    @Test
    void save_InvalidSeasonId_ThrowsException() {
        // Arrange
        PlayerStatEvents event = new PlayerStatEvents();
        event.setPlayerId(1L);
        event.setGameId(2L);
        event.setTeamId(3L);
        event.setSeasonId(999L); // Non-existent season ID
        event.setEventType("points_scored");
        event.setValue(2);
        event.setEventTime(Instant.now());
        event.setProcessedAt(Instant.now());
        event.setPlayerName("Test Player");

        // Act & Assert
        assertThrows(RuntimeException.class, () -> repository.save(event));
    }
} 