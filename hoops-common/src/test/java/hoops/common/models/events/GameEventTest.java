package hoops.common.models.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameEventTest {
    private static Validator validator;
    
    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidEvent() {
        PointsEvent event = new PointsEvent();
        event.setVersion(123L);
        event.setGameId("2024030100");
        event.setTeamId("BOS");
        event.setPlayerId("jt0");
        event.setEvent("point");
        event.setValue(3d);

        var violations = validator.validate(event);
        assertTrue(violations.isEmpty(), "Valid event should have no violations");
    }
    
    @Test
    void testMissingRequiredFields() {
        PointsEvent event = new PointsEvent();
        // Only set some fields
        event.setGameId("2024030100");
        event.setTeamId("BOS");

        var violations = validator.validate(event);
        assertEquals(3, violations.size(), "Should have violations for missing required fields");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Player ID is required")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Version is required")));
    }
    
    @Test
    void testBlankFields() {
        PointsEvent event = new PointsEvent();
        event.setVersion(1L);
        event.setGameId("");
        event.setTeamId(" ");
        event.setPlayerId("\t");
        event.setEvent(" ");
        event.setValue(3d);

        var violations = validator.validate(event);
        assertEquals(3, violations.size(), "Should have violations for blank fields");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Game ID is required")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Team ID is required")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Player ID is required")));
    }



} 