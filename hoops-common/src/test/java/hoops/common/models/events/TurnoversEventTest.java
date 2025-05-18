package hoops.common.models.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TurnoversEventTest {
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private TurnoversEvent createValidEvent() {
        TurnoversEvent event = new TurnoversEvent();
        event.setVersion(123L);
        event.setGameId("2024030100");
        event.setTeamId("BOS");
        event.setPlayerId("jt0");
        event.setEvent("turnover");
        return event;
    }

    @Test
    void testValidTurnovers() {
        TurnoversEvent event = createValidEvent();

        // Test valid turnover value
        event.setValue(1.0);
        var violations = validator.validate(event);
        assertTrue(violations.isEmpty(), "Should validate 1 turnover");

        // Test multiple turnovers (possible if tracking stats after the fact)
        event.setValue(2.0);
        violations = validator.validate(event);
        assertTrue(violations.isEmpty(), "Should validate multiple turnovers");
    }

    @Test
    void testInvalidTurnovers() {
        TurnoversEvent event = createValidEvent();

        // Test negative turnovers
        event.setValue(-1d);
        var violations = validator.validate(event);
        assertFalse(violations.isEmpty(), "Should not validate negative assists");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Turnovers must be at least 1")));

        // Test zero turnovers
        event.setValue(0d);
        violations = validator.validate(event);
        assertFalse(violations.isEmpty(), "Should not validate zero turnovers");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Turnovers must be at least 1")));

        // Test fractional turnovers
        event.setValue(1.5);
        violations = validator.validate(event);
        assertFalse(violations.isEmpty(), "Should not validate fractional turnovers");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Turnovers must be a whole number")));
    }

    @Test
    void testJsonDeserialization() throws Exception {
        var objectMapper = new ObjectMapper();
        // Create JSON representing a valid turnovers event
        String json = """
            {
                "version": 123,
                "gameId": "2024030100",
                "teamId": "BOS",
                "playerId": "jt0",
                "event": "turnover",
                "value": 1.0
            }
            """;

        // Deserialize JSON to TurnoversEvent object
        var event = objectMapper.readValue(json, GameEvent.class);
        // Verify properties were correctly deserialized
        assertEquals(123L, event.getVersion());
        assertEquals("2024030100", event.getGameId());
        assertEquals("BOS", event.getTeamId());
        assertEquals("jt0", event.getPlayerId());
        assertEquals("turnover", event.getEvent());
        assertEquals(1.0, event.getValue());

        // Validate the deserialized object
        var violations = validator.validate(event);
        assertTrue(violations.isEmpty(), "Deserialized event should be valid");
    }
}