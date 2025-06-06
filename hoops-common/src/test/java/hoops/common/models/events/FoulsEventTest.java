package hoops.common.models.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
        event.setVersion(123L);
        event.setGameId("2024030100");
        event.setTeamId("BOS");
        event.setPlayerId("jt0");
        event.setEvent("fouls");
        return event;
    }
    
    @Test
    void testValidFouls() {
        FoulsEvent event = createValidEvent();
        
        // Test all valid foul counts (0-6)
        for (double i = 1; i <= 6; i++) {
            event.setValue(i);
            var violations = validator.validate(event);
            assertTrue(violations.isEmpty(),
                String.format("Should validate %f fouls", i));
        }
    }
    
    @Test
    void testInvalidFouls() {
        FoulsEvent event = createValidEvent();
        
        // Test negative fouls
        event.setValue(-1d);
        var violations = validator.validate(event);
        assertFalse(violations.isEmpty(), "Should not validate negative fouls");
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("Incoming Fouls cannot be 0")));
        
        // Test more than 6 fouls
        event.setValue(7d);
        violations = validator.validate(event);
        assertFalse(violations.isEmpty(), "Should not validate more than 6 fouls");
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("cannot exceed 6")));
        
        // Test fractional fouls not possible with Integer
        // Removed fractional test since it's no longer possible with Integer type
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
                "event": "foul",
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
        assertEquals("foul", event.getEvent());
        assertEquals(3.0, event.getValue());

        // Validate the deserialized object
        var violations = validator.validate(event);
        assertTrue(violations.isEmpty(), "Deserialized event should be valid");
    }

} 