package hoops.ingestion.models.events;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Digits;

public class BlocksEvent extends GameEvent<Integer> {
    @Override
    @Min(value = 1, message = "Blocks must be at least 1")
    @Digits(integer = Integer.MAX_VALUE, fraction = 0, message = "Blocks must be a whole number")
    public Integer getValue() {
        return value;
    }
} 