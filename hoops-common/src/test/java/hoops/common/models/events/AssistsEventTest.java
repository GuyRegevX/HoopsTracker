package hoops.common.models.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import hoops.common.models.events.AssistsEvent;
import hoops.common.models.events.GameEvent;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AssistsEventTest {
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private AssistsEvent createValidEvent() {
        AssistsEvent event = new AssistsEvent();
        event.setVersion(123L);
        event.setGameId("2024030100");
        event.setTeamId("BOS");
        event.setPlayerId("jt0");
        event.setEvent("assist");
        return event;
    }

    @Test
    void testValidAssists() {
        AssistsEvent event = createValidEvent();

        // Test valid assist value
        event.setValue(1.0);
        var violations = validator.validate(event);
        assertTrue(violations.isEmpty(), "Should validate 1 assist");

        // Test multi-assists (possible in a single play)
        event.setValue(2.0);
        violations = validator.validate(event);
        assertTrue(violations.isEmpty(), "Should validate multiple assists");
    }

    @Test
    void testInvalidAssists() {
        AssistsEvent event = createValidEvent();

        // Test negative assists
        event.setValue(-1d);
        var violations = validator.validate(event);
        assertFalse(violations.isEmpty(), "Should not validate negative assists");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Assists must be at least 1")));

        // Test zero assists
        event.setValue(0d);
        violations = validator.validate(event);
        assertFalse(violations.isEmpty(), "Should not validate zero assists");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Assists must be at least 1")));

        // Test fractional assists
        event.setValue(1.5);
        violations = validator.validate(event);
        assertFalse(violations.isEmpty(), "Should not validate fractional assists");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Assists must be a whole number")));
    }

    @Test
    void testJsonDeserialization() throws Exception {
        var objectMapper = new ObjectMapper();
        // Create JSON representing a valid assists event
        String json = """
            {
                "version": 123,
                "gameId": "2024030100",
                "teamId": "BOS",
                "playerId": "jt0",
                "event": "assist",
                "value": 1.0
            }
            """;

        // Deserialize JSON to AssistsEvent object
        var event = objectMapper.readValue(json, GameEvent.class);
        // Verify properties were correctly deserialized
        assertEquals(123L, event.getVersion());
        assertEquals("2024030100", event.getGameId());
        assertEquals("BOS", event.getTeamId());
        assertEquals("jt0", event.getPlayerId());

        assertEquals("assist", event.getEvent());
        assertEquals(1.0, event.getValue());

        // Validate the deserialized object
        var violations = validator.validate(event);
        assertTrue(violations.isEmpty(), "Deserialized event should be valid");
    }
}