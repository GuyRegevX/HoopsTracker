package hoops.ingestion.models.events;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Digits;

public class MinutesPlayedEvent extends GameEvent<Double> {
    @Override
    @Min(value = 0, message = "Minutes played cannot be negative")
    @Max(value = 48, message = "Minutes played cannot exceed 48")
    @Digits(integer = 2, fraction = 1, message = "Minutes played can have at most 1 decimal place")
    public Double getValue() {
        return value;
    }
} 