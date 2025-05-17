package hoops.common.models.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Base class for all game events
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "event"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = PointsEvent.class, name = "point"),
    @JsonSubTypes.Type(value = ReboundsEvent.class, name = "rebound"),
    @JsonSubTypes.Type(value = AssistsEvent.class, name = "assist"),
    @JsonSubTypes.Type(value = BlocksEvent.class, name = "block"),
    @JsonSubTypes.Type(value = StealsEvent.class, name = "steal"),
    @JsonSubTypes.Type(value = TurnoversEvent.class, name = "turnover"),
    @JsonSubTypes.Type(value = FoulsEvent.class, name = "foul"),
    @JsonSubTypes.Type(value = MinutesPlayedEvent.class, name = "minutes_played")
})
@Data
public abstract class GameEvent {

    /**
     * event version
     */
    @NotNull(message = "Version is required")
    @Min(value = 1, message = "Version must be greater than 0")
    @JsonProperty(value = "version", required = true)
    protected Long version;

    /**
     * Game identifier. Must be a string under 10 characters for Redis compatibility.
     * Format: YYYYMMDDGGG (Year, Month, Day, Game Number)
     */
    @NotBlank(message = "Game ID is required")
    @JsonProperty(value = "gameId", required = true)
    protected String gameId;

    /**
     * Team identifier (e.g., "BOS" for Boston Celtics)
     */
    @NotBlank(message = "Team ID is required")
    @JsonProperty(value = "teamId", required = true)
    protected String teamId;
    
    /**
     * Player's unique identifier
     */
    @NotBlank(message = "Player ID is required")
    @JsonProperty(value = "playerId", required = true)
    protected String playerId;

    /**
     *  Game event one of:
     */
    @JsonProperty("event")
    private String event;

    /**
     * The value associated with this event (points scored, rebounds, etc.)
     */
    @JsonProperty(value = "value", required = true)
    @NotNull(message = "Value is required")
    protected Double value;
} 