package hoops.processor.services.seasons;

import hoops.processor.models.entities.Seasons;
import hoops.processor.repositories.seasons.SeasonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeasonServiceImpl implements SeasonService {
    private final SeasonRepository seasonRepository;

    @Override
    public Optional<Seasons> getCurrentSeason() {
        try {
            return seasonRepository.getActiveSession();
        } catch (Exception e) {
            log.error("Error retrieving current season", e);
            throw new RuntimeException("Failed to retrieve current season", e);
        }
    }
} 