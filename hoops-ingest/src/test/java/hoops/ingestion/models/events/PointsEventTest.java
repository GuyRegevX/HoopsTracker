package hoops.ingestion.models.events;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class PointsEventTest {
    private static Validator validator;
    
    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    private PointsEvent createValidEvent() {
        PointsEvent event = new PointsEvent();
        event.setGameId("2024030100");
        event.setTeamId("BOS");
        event.setPlayerId("jt0");
        event.setPlayerName("Jayson Tatum");
        event.setEvent("points");
        event.setTimestamp(Instant.now());
        return event;
    }
    
    @Test
    void testValidPoints() {
        PointsEvent event = createValidEvent();
        
        // Test all valid point values
        for (int points : new int[]{1, 2, 3}) {
            event.setValue(points);
            var violations = validator.validate(event);
            assertTrue(violations.isEmpty(), 
                String.format("Points value %d should be valid", points));
        }
    }
    
    @Test
    void testInvalidPoints() {
        PointsEvent event = createValidEvent();
        
        // Test invalid point values
        for (int points : new int[]{0, 4, -1}) {
            event.setValue(points);
            var violations = validator.validate(event);
            assertFalse(violations.isEmpty(),
                String.format("Points value %d should be invalid", points));
            
            if (points < 1) {
                assertTrue(violations.stream()
                    .anyMatch(v -> v.getMessage().contains("must be at least 1")));
            } else if (points > 3) {
                assertTrue(violations.stream()
                    .anyMatch(v -> v.getMessage().contains("cannot exceed 3")));
            }
        }
    }
} 