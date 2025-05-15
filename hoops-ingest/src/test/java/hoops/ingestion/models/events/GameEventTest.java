package hoops.ingestion.models.events;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class GameEventTest {
    private static Validator validator;
    
    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    @Test
    void testValidEvent() {
        PointsEvent event = new PointsEvent();
        event.setGameId("2024030100");
        event.setTeamId("BOS");
        event.setPlayerId("jt0");
        event.setPlayerName("Jayson Tatum");
        event.setEvent("points");
        event.setValue(3);
        event.setTimestamp(Instant.now());
        
        var violations = validator.validate(event);
        assertTrue(violations.isEmpty(), "Valid event should have no violations");
    }
    
    @Test
    void testMissingRequiredFields() {
        PointsEvent event = new PointsEvent();
        // Only set some fields
        event.setGameId("2024030100");
        event.setTeamId("BOS");
        
        var violations = validator.validate(event);
        assertEquals(5, violations.size(), "Should have violations for missing required fields");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Player ID is required")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Player name is required")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Event type is required")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Value is required")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Timestamp is required")));
    }
    
    @Test
    void testBlankFields() {
        PointsEvent event = new PointsEvent();
        event.setGameId("");
        event.setTeamId(" ");
        event.setPlayerId("\t");
        event.setPlayerName("");
        event.setEvent(" ");
        event.setValue(3);
        event.setTimestamp(Instant.now());
        
        var violations = validator.validate(event);
        assertEquals(5, violations.size(), "Should have violations for blank fields");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Game ID is required")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Team ID is required")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Player ID is required")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Player name is required")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Event type is required")));
    }
    
    @Test
    void testFutureTimestamp() {
        PointsEvent event = new PointsEvent();
        event.setGameId("2024030100");
        event.setTeamId("BOS");
        event.setPlayerId("jt0");
        event.setPlayerName("Jayson Tatum");
        event.setEvent("points");
        event.setValue(3);
        event.setTimestamp(Instant.now().plusSeconds(3600)); // 1 hour in future
        
        var violations = validator.validate(event);
        assertEquals(1, violations.size(), "Should have violation for future timestamp");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("must not be in the future")));
    }
} 