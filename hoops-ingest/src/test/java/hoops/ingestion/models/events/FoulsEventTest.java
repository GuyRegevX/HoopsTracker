package hoops.ingestion.models.events;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class FoulsEventTest {
    private static Validator validator;
    
    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    private FoulsEvent createValidEvent() {
        FoulsEvent event = new FoulsEvent();
        event.setGameId("2024030100");
        event.setTeamId("BOS");
        event.setPlayerId("jt0");
        event.setPlayerName("Jayson Tatum");
        event.setEvent("fouls");
        event.setTimestamp(Instant.now());
        return event;
    }
    
    @Test
    void testValidFouls() {
        FoulsEvent event = createValidEvent();
        
        // Test all valid foul counts (0-6)
        for (int i = 0; i <= 6; i++) {
            event.setValue(i);
            var violations = validator.validate(event);
            assertTrue(violations.isEmpty(), 
                String.format("Should validate %d fouls", i));
        }
    }
    
    @Test
    void testInvalidFouls() {
        FoulsEvent event = createValidEvent();
        
        // Test negative fouls
        event.setValue(-1);
        var violations = validator.validate(event);
        assertFalse(violations.isEmpty(), "Should not validate negative fouls");
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("cannot be negative")));
        
        // Test more than 6 fouls
        event.setValue(7);
        violations = validator.validate(event);
        assertFalse(violations.isEmpty(), "Should not validate more than 6 fouls");
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("cannot exceed 6")));
        
        // Test fractional fouls not possible with Integer
        // Removed fractional test since it's no longer possible with Integer type
    }
} 