package hoops.ingestion.controllers;

import hoops.ingestion.models.events.GameEvent;
import hoops.ingestion.services.GameEventService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ingest")
public class GameDataController {
    private static final Logger logger = LoggerFactory.getLogger(GameDataController.class);
    
    private final GameEventService gameEventService;
    
    @Autowired
    public GameDataController(GameEventService gameEventService) {
        this.gameEventService = gameEventService;
    }
    
    @PostMapping("/event")
    public ResponseEntity<?> ingestGameEvent(@Valid @RequestBody GameEvent gameEvent) {
        logger.info("Received game event for game ID: {}, player: {}, event: {}", 
            gameEvent.getGameId(), gameEvent.getPlayerName(), gameEvent.getEvent());
        
        try {
            gameEventService.processGameEvent(gameEvent);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error processing game event", e);
            return ResponseEntity.internalServerError().body("Error processing game event");
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, String>> errors = new ArrayList<>();
        
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            Map<String, String> errorDetails = new HashMap<>();
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errorDetails.put("field", fieldName);
            errorDetails.put("message", errorMessage);
            errors.add(errorDetails);
        });
        
        response.put("errors", errors);
        return ResponseEntity.badRequest().body(response);
    }
} 