package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.VarietyResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import com.infineonbit.sustainablefarm.modules.plants.exception.VarietyNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VarietyServiceTest {

    @Mock
    private VarietyRepository varietyRepository;

    @InjectMocks
    private VarietyService varietyService;

    /**
     * The only real row of the project: Zalka 2025. The four undocumented
     * fields stay null here too, so the tests also pin down that the service
     * does not invent a value for them.
     */
    private static Variety keittOnBlockA() {
        return new Variety(
                1L,
                null,
                "Keitt",
                200,
                8.0,
                8.0,
                null,
                44000.0,
                null,
                null,
                "A",
                null,
                "Zalka_2025",
                Instant.parse("2025-01-01T00:00:00Z"));
    }

    @Test
    void obtainAllVarieties_shouldReturnMatchingVarieties_whenFilterMatches() {
        // Arrange
        when(varietyRepository.findByOptionalFilters(null, "A"))
                .thenReturn(List.of(keittOnBlockA()));
        // Act
        List<VarietyResponse> varietyResponses = varietyService.obtainAllVarieties(null, "A");
        // Assert
        assertEquals(1, varietyResponses.size());
        assertEquals("Keitt", varietyResponses.get(0).nom());
        assertEquals("A", varietyResponses.get(0).bloc_parcelle());
        assertEquals(200, varietyResponses.get(0).nombre_arbres());
        assertNull(varietyResponses.get(0).rendement_reel_kg());
        assertNull(varietyResponses.get(0).vigueur());
    }

    @Test
    void obtainAllVarieties_shouldReturnEmptyList_whenFilterMatchesNothing() {
        // Arrange
        when(varietyRepository.findByOptionalFilters(null, "ZZZ"))
                .thenReturn(List.of());
        // Act
        List<VarietyResponse> varietyResponses = varietyService.obtainAllVarieties(null, "ZZZ");
        // Assert
        assertTrue(varietyResponses.isEmpty());
    }

    @Test
    void obtainVarietyById_shouldReturnVariety_whenIdExists() {
        // Arrange
        when(varietyRepository.findById(1L)).thenReturn(Optional.of(keittOnBlockA()));
        // Act
        VarietyResponse varietyResponse = varietyService.obtainVarietyById(1L);
        // Assert
        assertEquals(1L, varietyResponse.id());
        assertEquals("Keitt", varietyResponse.nom());
        assertEquals(44000.0, varietyResponse.rendement_attendu_kg());
        assertEquals("Zalka_2025", varietyResponse.source());
    }

    @Test
    void obtainVarietyById_shouldThrowException_whenIdDoesNotExist() {
        // Arrange
        Long nonExistentId = 99L;
        when(varietyRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        // Act
        VarietyNotFoundException ex = assertThrows(VarietyNotFoundException.class, () -> {
            varietyService.obtainVarietyById(nonExistentId);
        });
        // Assert
        assertEquals("Variety with ID 99 not found", ex.getMessage());
    }
}
