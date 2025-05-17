package hoops.common.models.events;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class StatEventTest {
    private static Validator validator;
    
    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    private <T extends GameEvent> T createValidEvent(Supplier<T> constructor, String eventType) {
        T event = constructor.get();
        event.setVersion(2024030100L);
        event.setGameId("2024030100");
        event.setTeamId("BOS");
        event.setPlayerId("jt0");
        event.setEvent(eventType);
        return event;
    }

    @Test
    void testVersionValidation() {
        GameEvent event = createValidEvent(PointsEvent::new, "point");
        event.setValue(3d);
        // Test valid versions
        event.setVersion(1L);
        var violations = validator.validate(event);
        assertTrue(violations.isEmpty(), "Should validate version 1");

        event.setVersion(999L);
        violations = validator.validate(event);
        assertTrue(violations.isEmpty(), "Should validate larger version numbers");

        event.setVersion(null);
        violations = validator.validate(event);
        assertFalse(violations.isEmpty(), "Should not validate empty version");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Version is required")));
    }

    @Test
    void testReboundsValidation() {
        ReboundsEvent event = createValidEvent(ReboundsEvent::new, "rebounds");
        
        // Test valid rebounds
        event.setValue(1d);
        var violations = validator.validate(event);
        assertTrue(violations.isEmpty(), "Should validate 1 rebound");
        
        event.setValue(10d);
        violations = validator.validate(event);
        assertTrue(violations.isEmpty(), "Should validate multiple rebounds");
        
        // Test invalid rebounds
        event.setValue(0d);
        violations = validator.validate(event);
        assertFalse(violations.isEmpty(), "Should not validate 0 rebounds");
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("must be at least 1")));
        
        event.setValue(-1d);
        violations = validator.validate(event);
        assertFalse(violations.isEmpty(), "Should not validate negative rebounds");
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("must be at least 1")));
    }
    
    @Test
    void testAssistsValidation() {
        AssistsEvent event = createValidEvent(AssistsEvent::new, "assists");
        
        // Test valid assists
        event.setValue(1d);
        var violations = validator.validate(event);
        assertTrue(violations.isEmpty(), "Should validate 1 assist");
        
        event.setValue(15d);
        violations = validator.validate(event);
        assertTrue(violations.isEmpty(), "Should validate multiple assists");
        
        // Test invalid assists
        event.setValue(0d);
        violations = validator.validate(event);
        assertFalse(violations.isEmpty(), "Should not validate 0 assists");
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("must be at least 1")));
        
        event.setValue(-1d);
        violations = validator.validate(event);
        assertFalse(violations.isEmpty(), "Should not validate negative assists");
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("must be at least 1")));
    }
    
    @Test
    void testStealsValidation() {
        StealsEvent event = createValidEvent(StealsEvent::new, "steals");
        
        // Test valid steals
        event.setValue(1d);
        var violations = validator.validate(event);
        assertTrue(violations.isEmpty(), "Should validate 1 steal");
        
        event.setValue(5d);
        violations = validator.validate(event);
        assertTrue(violations.isEmpty(), "Should validate multiple steals");
        
        // Test invalid steals
        event.setValue(0d);
        violations = validator.validate(event);
        assertFalse(violations.isEmpty(), "Should not validate 0 steals");
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("must be at least 1")));
        
        event.setValue(-1d);
        violations = validator.validate(event);
        assertFalse(violations.isEmpty(), "Should not validate negative steals");
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("must be at least 1")));
    }
    
    @Test
    void testBlocksValidation() {
        BlocksEvent event = createValidEvent(BlocksEvent::new, "blocks");
        
        // Test valid blocks
        event.setValue(1d);
        var violations = validator.validate(event);
        assertTrue(violations.isEmpty(), "Should validate 1 block");
        
        event.setValue(7d);
        violations = validator.validate(event);
        assertTrue(violations.isEmpty(), "Should validate multiple blocks");
        
        // Test invalid blocks
        event.setValue(0d);
        violations = validator.validate(event);
        assertFalse(violations.isEmpty(), "Should not validate 0 blocks");
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("must be at least 1")));
        
        event.setValue(-1d);
        violations = validator.validate(event);
        assertFalse(violations.isEmpty(), "Should not validate negative blocks");
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("must be at least 1")));
    }
}