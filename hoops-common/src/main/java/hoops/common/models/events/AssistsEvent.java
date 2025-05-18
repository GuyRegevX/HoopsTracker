package hoops.common.models.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Digits;

public class AssistsEvent extends GameEvent{
    @Override
    @Min(value = 1, message = "Assists must be at least 1")
    @Digits(integer = Integer.MAX_VALUE, fraction = 0, message = "Assists must be a whole number")
    public Double getValue() {
        return value;
    }

    @Override
    @JsonProperty("assist")
    public String getEvent() {
        return "assist"; // Returning the correct event type
    }
} 