package hoops.ingestion.models.events;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Digits;

public class FoulsEvent extends GameEvent<Integer> {
    @Override
    @Min(value = 0, message = "Fouls cannot be negative")
    @Max(value = 6, message = "Fouls cannot exceed 6")
    @Digits(integer = 1, fraction = 0, message = "Fouls must be a whole number")
    public Integer getValue() {
        return value;
    }
} 