package hoops.processor.services.season;

import hoops.processor.models.entities.Seasons;
import hoops.processor.repositories.seasons.SeasonRepository;
import hoops.processor.services.seasons.SeasonServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = SeasonServiceImpl.class)
class SeasonServiceImplTest {

    @MockBean
    private SeasonRepository seasonRepository;

    @Autowired
    private SeasonServiceImpl seasonService;

    @Test
    void getCurrentSeason_WhenSeasonExists_ReturnsSeason() {
        // Arrange
        Seasons expectedSeason = Seasons.builder()
                .id("season1")
                .name("2023-2024")
                .isActive(true)
                .build();
        when(seasonRepository.getActiveSession()).thenReturn(Optional.of(expectedSeason));

        // Act
        Optional<Seasons> result = seasonService.getCurrentSeason();

        // Assert
        assertTrue(result.isPresent());
        assertEquals(expectedSeason.getId(), result.get().getId());
        assertEquals(expectedSeason.getName(), result.get().getName());
        assertTrue(result.get().isActive());
        verify(seasonRepository).getActiveSession();
    }

    @Test
    void getCurrentSeason_WhenNoSeason_ReturnsEmpty() {
        // Arrange
        when(seasonRepository.getActiveSession()).thenReturn(Optional.empty());

        // Act
        Optional<Seasons> result = seasonService.getCurrentSeason();

        // Assert
        assertTrue(result.isEmpty());
        verify(seasonRepository).getActiveSession();
    }

    @Test
    void getCurrentSeason_WhenRepositoryThrowsException_ThrowsRuntimeException() {
        // Arrange
        when(seasonRepository.getActiveSession()).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> seasonService.getCurrentSeason());
        assertEquals("Failed to retrieve current season", exception.getMessage());
        verify(seasonRepository).getActiveSession();
    }

}