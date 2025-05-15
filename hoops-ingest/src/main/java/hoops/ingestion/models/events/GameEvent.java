package hoops.ingestion.models.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import java.time.Instant;

/**
 * Base class for all game events
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = PointsScoredEvent.class, name = "points_scored"),
    @JsonSubTypes.Type(value = ReboundEvent.class, name = "rebound"),
    @JsonSubTypes.Type(value = AssistEvent.class, name = "assist"),
    @JsonSubTypes.Type(value = BlockEvent.class, name = "block"),
    @JsonSubTypes.Type(value = StealEvent.class, name = "steal"),
    @JsonSubTypes.Type(value = TurnoverEvent.class, name = "turnover"),
    @JsonSubTypes.Type(value = FoulEvent.class, name = "foul"),
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
     * Player's name for display purposes
     */
    @JsonProperty("playerName")
    private String playerName;
    
    /**
     * When the event occurred in the game
     */
    @PastOrPresent(message = "Event time must be in the past or present")
    @JsonProperty("eventTime")
    private Instant eventTime;
    
    /**
     * The value associated with this event (points scored, rebounds, etc.)
     */
    @JsonProperty(value = "value", required = true)
    protected T value;
    
    // Standard getters and setters
    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }
    
    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }
    
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    
    public Instant getEventTime() { return eventTime; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }
    
    public T getValue() { return value; }
    public void setValue(T value) { this.value = value; }
} 