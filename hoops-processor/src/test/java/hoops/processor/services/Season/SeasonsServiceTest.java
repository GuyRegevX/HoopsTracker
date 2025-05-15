package hoops.processor.services.Season;

import hoops.processor.models.entities.Seasons;
import hoops.processor.repositories.Season.SeasonsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeasonsServiceTest {

    @Mock
    private SeasonsRepository seasonsRepository;

    @InjectMocks
    private SeasonsServiceImpl seasonsService;

    private Seasons activeSeason;

    @BeforeEach
    void setUp() {
        // Setup test data
        activeSeason = new Seasons();
        activeSeason.setId(1L);
        activeSeason.setName("2023-2024");
        activeSeason.setActive(true);
    }

    @Test
    void getCurrentSeason_Success() {
        // Arrange
        when(seasonsRepository.getCurrentSeason()).thenReturn(Optional.of(activeSeason));

        // Act
        Optional<Seasons> result = seasonsService.getCurrentSeason();

        // Assert
        assertTrue(result.isPresent());
        assertEquals(activeSeason.getId(), result.get().getId());
        assertEquals(activeSeason.getName(), result.get().getName());
        assertEquals(activeSeason.isActive(), result.get().isActive());
        verify(seasonsRepository).getCurrentSeason();
    }

    @Test
    void getCurrentSeason_NoActiveSeason() {
        // Arrange
        when(seasonsRepository.getCurrentSeason()).thenReturn(Optional.empty());

        // Act
        Optional<Seasons> result = seasonsService.getCurrentSeason();

        // Assert
        assertFalse(result.isPresent());
        verify(seasonsRepository).getCurrentSeason();
    }

    @Test
    void getCurrentSeason_RepositoryError() {
        // Arrange
        when(seasonsRepository.getCurrentSeason()).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> seasonsService.getCurrentSeason());
        assertEquals("Database error", exception.getMessage());
        verify(seasonsRepository).getCurrentSeason();
    }
} 