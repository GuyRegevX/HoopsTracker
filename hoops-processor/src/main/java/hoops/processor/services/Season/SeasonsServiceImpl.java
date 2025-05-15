package hoops.processor.services.Season;

import hoops.processor.models.entities.Seasons;
import hoops.processor.repositories.Season.SeasonsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeasonsServiceImpl implements SeasonsService {
    private final SeasonsRepository seasonsRepository;

    @Override
    public Optional<Seasons> getCurrentSeason() {
        try {
            return seasonsRepository.getCurrentSeason();
        } catch (Exception e) {
            log.error("Error getting current season", e);
            throw e;
        }
    }
} 