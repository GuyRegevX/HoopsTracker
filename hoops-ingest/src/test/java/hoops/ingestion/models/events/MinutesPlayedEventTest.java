package hoops.ingestion.models.events;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class MinutesPlayedEventTest {
    private static Validator validator;
    
    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    private MinutesPlayedEvent createValidEvent() {
        MinutesPlayedEvent event = new MinutesPlayedEvent();
        event.setGameId("2024030100");
        event.setTeamId("BOS");
        event.setPlayerId("jt0");
        event.setPlayerName("Jayson Tatum");
        event.setEvent("minutes_played");
        event.setTimestamp(Instant.now());
        return event;
    }
    
    @Test
    void testValidMinutes() {
        MinutesPlayedEvent event = createValidEvent();
        
        // Test 0 minutes
        event.setValue(0.0);
        var violations = validator.validate(event);
        assertTrue(violations.isEmpty(), "Should validate 0 minutes");
        
        // Test typical minutes
        event.setValue(24.5);
        violations = validator.validate(event);
        assertTrue(violations.isEmpty(), "Should validate typical minutes");
        
        // Test maximum minutes
        event.setValue(48.0);
        violations = validator.validate(event);
        assertTrue(violations.isEmpty(), "Should validate maximum minutes");
    }
    
    @Test
    void testInvalidMinutes() {
        MinutesPlayedEvent event = createValidEvent();
        
        // Test negative minutes
        event.setValue(-1.0);
        var violations = validator.validate(event);
        assertFalse(violations.isEmpty(), "Should not validate negative minutes");
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("cannot be negative")));
        
        // Test more than 48 minutes
        event.setValue(48.1);
        violations = validator.validate(event);
        assertFalse(violations.isEmpty(), "Should not validate more than 48 minutes");
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("cannot exceed 48")));
        
        // Test extremely high minutes
        event.setValue(100.0);
        violations = validator.validate(event);
        assertFalse(violations.isEmpty(), "Should not validate extremely high minutes");
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("cannot exceed 48")));
    }
} 