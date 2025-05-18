package hoops.common.models.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StealsEventTest {
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private StealsEvent createValidEvent() {
        StealsEvent event = new StealsEvent();
        event.setVersion(123L);
        event.setGameId("2024030100");
        event.setTeamId("BOS");
        event.setPlayerId("jt0");
        event.setEvent("steal");
        return event;
    }

    @Test
    void testValidSteals() {
        StealsEvent event = createValidEvent();

        // Test valid steal value
        event.setValue(1.0);
        var violations = validator.validate(event);
        assertTrue(violations.isEmpty(), "Should validate 1 steal");

        // Test multiple steals (possible if tracking stats after the fact)
        event.setValue(2.0);
        violations = validator.validate(event);
        assertTrue(violations.isEmpty(), "Should validate multiple steals");
    }

    @Test
    void testInvalidSteals() {
        StealsEvent event = createValidEvent();

        // Test negative steals
        event.setValue(-1d);
        var violations = validator.validate(event);
        assertFalse(violations.isEmpty(), "Should not validate negative steals");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Steals must be at least 1")));

        // Test zero steals
        event.setValue(0d);
        violations = validator.validate(event);
        assertFalse(violations.isEmpty(), "Should not validate zero steals");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Steals must be at least 1")));

        // Test fractional steals
        event.setValue(1.5);
        violations = validator.validate(event);
        assertFalse(violations.isEmpty(), "Should not validate fractional steals");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Steals must be a whole number")));
    }

    @Test
    void testJsonDeserialization() throws Exception {
        var objectMapper = new ObjectMapper();
        // Create JSON representing a valid steals event
        String json = """
            {
                "version": 123,
                "gameId": "2024030100",
                "teamId": "BOS",
                "playerId": "jt0",
                "event": "steal",
                "value": 1.0
            }
            """;

        // Deserialize JSON to StealsEvent object
        var event = objectMapper.readValue(json, GameEvent.class);
        // Verify properties were correctly deserialized
        assertEquals(123L, event.getVersion());
        assertEquals("2024030100", event.getGameId());
        assertEquals("BOS", event.getTeamId());
        assertEquals("jt0", event.getPlayerId());
        assertEquals("steal", event.getEvent());
        assertEquals(1.0, event.getValue());

        // Validate the deserialized object
        var violations = validator.validate(event);
        assertTrue(violations.isEmpty(), "Deserialized event should be valid");
    }
}