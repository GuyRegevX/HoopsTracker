package hoops.processor.models.entities;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;

@Builder
@AllArgsConstructor
@Data
public class Seasons {
    private String id;
    private String name;
    private boolean isActive;
}