package hoops.ingestion.models.events;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Digits;

public class AssistsEvent extends GameEvent<Integer> {
    @Override
    @Min(value = 1, message = "Assists must be at least 1")
    @Digits(integer = Integer.MAX_VALUE, fraction = 0, message = "Assists must be a whole number")
    public Integer getValue() {
        return value;
    }
} 