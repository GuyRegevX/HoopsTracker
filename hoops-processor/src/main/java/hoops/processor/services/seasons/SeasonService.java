package hoops.processor.services.seasons;

import hoops.processor.models.entities.Seasons;
import java.util.Optional;

public interface SeasonService {
    Optional<Seasons> getCurrentSeason();
} 