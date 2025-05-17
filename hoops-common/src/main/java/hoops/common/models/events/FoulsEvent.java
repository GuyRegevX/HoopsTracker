package hoops.common.models.events;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Digits;

public class FoulsEvent extends GameEvent {
    @Override
    @Min(value = 1, message = "Incoming Fouls cannot be 0")
    @Max(value = 6, message = "Fouls cannot exceed 6")
    @Digits(integer = 1, fraction = 0, message = "Fouls must be a whole number")
    public Double getValue() {
        return value;
    }
} 