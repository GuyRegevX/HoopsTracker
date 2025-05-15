package hoops.ingestion.models.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.Instant;

/**
 * Base class for all basketball game events.
 * Uses JSON type information to deserialize specific event types based on the "event" field.
 * Each event type (points, rebounds, etc.) extends this class with specific validation rules.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "event",
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = PointsEvent.class, name = "points"),
    @JsonSubTypes.Type(value = ReboundsEvent.class, name = "rebounds"),
    @JsonSubTypes.Type(value = AssistsEvent.class, name = "assists"),
    @JsonSubTypes.Type(value = StealsEvent.class, name = "steals"),
    @JsonSubTypes.Type(value = BlocksEvent.class, name = "blocks"),
    @JsonSubTypes.Type(value = FoulsEvent.class, name = "fouls"),
    @JsonSubTypes.Type(value = MinutesPlayedEvent.class, name = "minutes_played")
})
public abstract class GameEvent<T extends Number> {
    /**
     * Game identifier. Must be a string under 10 characters for Redis compatibility.
     * Format: YYYYMMDDGGG (Year, Month, Day, Game Number)
     */
    @NotBlank(message = "Game ID is required")
    @JsonProperty(value = "gameId", required = true)
    private String gameId;
    
    /**
     * Team identifier (e.g., "BOS" for Boston Celtics)
     */
    @NotBlank(message = "Team ID is required")
    @JsonProperty(value = "teamId", required = true)
    private String teamId;
    
    /**
     * Player's unique identifier
     */
    @NotBlank(message = "Player ID is required")
    @JsonProperty(value = "playerId", required = true)
    private String playerId;
    
    /**
     * Player's full name
     */
    @NotBlank(message = "Player name is required")
    @JsonProperty(value = "playerName", required = true)
    private String playerName;
    
    /**
     * Type of event (points, rebounds, etc.)
     * This field is used for JSON deserialization to determine the concrete event type
     */
    @NotBlank(message = "Event type is required")
    @JsonProperty(value = "event", required = true)
    private String event;
    
    /**
     * Event value (points scored, number of rebounds, etc.)
     * Specific validation rules are defined in concrete event classes
     */
    @NotNull(message = "Value is required")
    @JsonProperty(value = "value", required = true)
    protected T value;
    
    /**
     * Event timestamp in ISO-8601 format
     * Must not be in the future
     */
    @NotNull(message = "Timestamp is required")
    @PastOrPresent(message = "Timestamp must not be in the future")
    @JsonProperty(value = "timestamp", required = true)
    private Instant timestamp;
    
    // Standard getters and setters
    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }
    
    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }
    
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    
    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }
    
    public T getValue() { return value; }
    public void setValue(T value) { this.value = value; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
} 