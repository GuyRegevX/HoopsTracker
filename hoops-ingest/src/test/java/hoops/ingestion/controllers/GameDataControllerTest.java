package hoops.ingestion.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import hoops.ingestion.models.events.*;
import hoops.ingestion.services.GameEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GameDataController.class)
class GameDataControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private GameEventService gameEventService;

    private static Stream<Arguments> validEventProvider() {
        return Stream.of(
            Arguments.of(createEvent(PointsEvent::new, "points", 3)),
            Arguments.of(createEvent(ReboundsEvent::new, "rebounds", 5)),
            Arguments.of(createEvent(AssistsEvent::new, "assists", 8)),
            Arguments.of(createEvent(BlocksEvent::new, "blocks", 2)),
            Arguments.of(createEvent(StealsEvent::new, "steals", 3)),
            Arguments.of(createEvent(FoulsEvent::new, "fouls", 2)),
            Arguments.of(createEvent(MinutesPlayedEvent::new, "minutes_played", 24.5))
        );
    }

    private static <T extends GameEvent> T createEvent(EventSupplier<T> supplier, String eventType, Number value) {
        T event = supplier.get();
        event.setGameId("2024030100");
        event.setTeamId("BOS");
        event.setPlayerId("jt0");
        event.setPlayerName("Jayson Tatum");
        event.setEvent(eventType);
        event.setValue(value);
        event.setTimestamp(Instant.now());
        return event;
    }

    @FunctionalInterface
    interface EventSupplier<T extends GameEvent> {
        T get();
    }
    
    @ParameterizedTest
    @MethodSource("validEventProvider")
    void testValidEventIngestion(GameEvent event) throws Exception {
        // Configure mock
        doNothing().when(gameEventService).processGameEvent(any(GameEvent.class));
        
        // Perform request
        mockMvc.perform(post("/api/v1/ingest/event")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
            .andExpect(status().isOk());
        
        // Verify service was called
        verify(gameEventService, times(1)).processGameEvent(any(GameEvent.class));
    }

    @Test
    void testInvalidEventMissingFields() throws Exception {
        // Create invalid event (missing required fields)
        PointsEvent event = new PointsEvent();
        event.setGameId("2024030100");
        
        // Perform request
        mockMvc.perform(post("/api/v1/ingest/event")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors").isArray())
            .andExpect(jsonPath("$.errors[?(@.field=='teamId')].message").exists())
            .andExpect(jsonPath("$.errors[?(@.field=='playerId')].message").exists())
            .andExpect(jsonPath("$.errors[?(@.field=='playerName')].message").exists())
            .andExpect(jsonPath("$.errors[?(@.field=='value')].message").exists());
        
        // Verify service was not called
        verify(gameEventService, never()).processGameEvent(any());
    }

    @Test
    void testInvalidEventValues() throws Exception {
        // Test invalid points value
        PointsEvent event = createEvent(PointsEvent::new, "points", 4); // Invalid: points must be 1-3
        
        mockMvc.perform(post("/api/v1/ingest/event")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors").isArray())
            .andExpect(jsonPath("$.errors[?(@.field=='value')].message").value("Points cannot exceed 3"));

        // Test invalid minutes value
        MinutesPlayedEvent minutesEvent = createEvent(MinutesPlayedEvent::new, "minutes_played", 60.0); // Invalid: max 48
        
        mockMvc.perform(post("/api/v1/ingest/event")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(minutesEvent)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors").isArray())
            .andExpect(jsonPath("$.errors[?(@.field=='value')].message").value("Minutes played cannot exceed 48"));
    }
    
    @Test
    void testServiceError() throws Exception {
        GameEvent event = createEvent(PointsEvent::new, "points", 3);
        
        // Configure mock to throw exception
        doThrow(new RuntimeException("Test error"))
            .when(gameEventService).processGameEvent(any());
        
        // Perform request
        mockMvc.perform(post("/api/v1/ingest/event")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
            .andExpect(status().isInternalServerError())
            .andExpect(content().string("Error processing game event"));
    }

    @Test
    void testInvalidJson() throws Exception {
        String invalidJson = "{invalid json}";
        
        mockMvc.perform(post("/api/v1/ingest/event")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());
    }
} 