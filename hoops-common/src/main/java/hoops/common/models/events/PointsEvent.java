package hoops.common.models.events;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Digits;

public class PointsEvent extends GameEvent {
    @Override
    @Min(value = 1, message = "Points must be at least 1")
    @Max(value = 3, message = "Points cannot exceed 3")
    @Digits(integer = 1, fraction = 0, message = "Points must be a whole number")
    public Double getValue() {
        return value;
    }
} 