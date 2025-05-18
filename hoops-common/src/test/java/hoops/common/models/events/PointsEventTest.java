package hoops.common.models.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
        event.setVersion(2024030100l);
        event.setGameId("2024030100");
        event.setTeamId("BOS");
        event.setPlayerId("jt0");
        event.setEvent("point");
        return event;
    }
    
    @Test
    void testValidPoints() {
        PointsEvent event = createValidEvent();
        
        // Test all valid point values
        for (double points : new int[]{1, 2, 3}) {
            event.setValue(points);
            var violations = validator.validate(event);
            assertTrue(violations.isEmpty(), 
                String.format("Points value %f should be valid", points));
        }
    }
    
    @Test
    void testInvalidPoints() {
        PointsEvent event = createValidEvent();
        
        // Test invalid point values
        for (double points : new int[]{0, 4, -1}) {
            event.setValue(points);
            var violations = validator.validate(event);
            assertFalse(violations.isEmpty(),
                String.format("Points value %f should be invalid", points));
            
            if (points < 1) {
                assertTrue(violations.stream()
                    .anyMatch(v -> v.getMessage().contains("must be at least 1")));
            } else if (points > 3) {
                assertTrue(violations.stream()
                    .anyMatch(v -> v.getMessage().contains("cannot exceed 3")));
            }
        }
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
                "event": "point",
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
        assertEquals("point", event.getEvent());
        assertEquals(3.0, event.getValue());

        // Validate the deserialized object
        var violations = validator.validate(event);
        assertTrue(violations.isEmpty(), "Deserialized event should be valid");
    }
} 