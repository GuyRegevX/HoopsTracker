package hoops.processor.repositories.seasons;

import hoops.processor.models.entities.Seasons;

import java.util.Optional;

public interface SeasonRepository {
    Optional<Seasons> getActiveSession();
}