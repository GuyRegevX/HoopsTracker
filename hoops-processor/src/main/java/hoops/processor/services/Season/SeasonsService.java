package hoops.processor.services.Season;

import hoops.processor.models.entities.Seasons;
import java.util.Optional;

public interface SeasonsService {
    Optional<Seasons> getCurrentSeason();
} 