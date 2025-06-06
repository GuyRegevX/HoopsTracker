package hoops.common.models.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
        event.setVersion(123L);
        event.setGameId("2024030100");
        event.setTeamId("BOS");
        event.setPlayerId("jt0");
        event.setEvent("minutes_played");
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

    @Test
    void testJsonDeserialization() throws Exception {
        var objectMapper = new ObjectMapper();
        // Create JSON representing a valid fouls event
        String json = """
            {
                "version": 123,
                "gameId": "2024030100",
                "teamId": "BOS",
                "playerId": "jt0",
                "event": "minutes_played",
                "value": 3.0
            }
            """;

        // Deserialize JSON to FoulsEvent object
        var event = objectMapper.readValue(json, GameEvent.class);
        // Verify properties were correctly deserialized
        assertEquals(123L, event.getVersion());
        assertEquals("2024030100", event.getGameId());
        assertEquals("BOS", event.getTeamId());
        assertEquals("jt0", event.getPlayerId());
        assertEquals("minutes_played", event.getEvent());
        assertEquals(3.0, event.getValue());

        // Validate the deserialized object
        var violations = validator.validate(event);
        assertTrue(violations.isEmpty(), "Deserialized event should be valid");
    }
} 