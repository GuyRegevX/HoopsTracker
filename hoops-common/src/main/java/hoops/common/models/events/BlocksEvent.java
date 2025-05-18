package hoops.common.models.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Digits;

public class BlocksEvent extends GameEvent {
    @Override
    @Min(value = 1, message = "Blocks must be at least 1")
    @Digits(integer = Integer.MAX_VALUE, fraction = 0, message = "Blocks must be a whole number")
    public Double getValue() {
        return value;
    }

    @Override
    @JsonProperty("event")
    public String getEvent() {
        return "block";
    }
} 