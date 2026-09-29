package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.VarietyResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import com.infineonbit.sustainablefarm.modules.plants.exception.VarietyNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.PopulationEventRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.PopulationEventRepository.TreeBalance;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VarietyServiceTest {

    @Mock
    private VarietyRepository varietyRepository;

    @Mock
    private PopulationEventRepository populationEventRepository;

    @InjectMocks
    private VarietyService varietyService;

    private static TreeBalance balance(Long varietyId, Long balance) {
        return new TreeBalance() {
            @Override
            public Long getVarietyId() {
                return varietyId;
            }

            @Override
            public Long getBalance() {
                return balance;
            }
        };
    }

    private static Variety plantedVariety(Long id, String name) {
        Variety variety = new Variety();
        variety.setId(id);
        variety.setName(name);
        variety.setBlockCode("B");
        variety.setTreeCount(150);
        variety.setSource("user_entry");
        return variety;
    }

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
    void getAllVarieties_shouldReturnMatchingVarieties_whenFilterMatches() {
        // Arrange
        when(varietyRepository.findByOptionalFilters(null, "A"))
                .thenReturn(List.of(keittOnBlockA()));
        // Act
        List<VarietyResponse> varietyResponses = varietyService.getAllVarieties(null, "A");
        // Assert
        assertEquals(1, varietyResponses.size());
        assertEquals("Keitt", varietyResponses.get(0).name());
        assertEquals("A", varietyResponses.get(0).blockCode());
        assertEquals(200, varietyResponses.get(0).treeCount());
        assertNull(varietyResponses.get(0).actualYieldKg());
        assertNull(varietyResponses.get(0).vigor());
    }

    @Test
    void getAllVarieties_shouldReturnEmptyList_whenFilterMatchesNothing() {
        // Arrange
        when(varietyRepository.findByOptionalFilters(null, "ZZZ"))
                .thenReturn(List.of());
        // Act
        List<VarietyResponse> varietyResponses = varietyService.getAllVarieties(null, "ZZZ");
        // Assert
        assertTrue(varietyResponses.isEmpty());
    }

    @Test
    void getVarietyById_shouldReturnVariety_whenIdExists() {
        // Arrange
        when(varietyRepository.findById(1L)).thenReturn(Optional.of(keittOnBlockA()));
        // Act
        VarietyResponse varietyResponse = varietyService.getVarietyById(1L);
        // Assert
        assertEquals(1L, varietyResponse.id());
        assertEquals("Keitt", varietyResponse.name());
        assertEquals(44000.0, varietyResponse.expectedYieldKg());
        assertEquals("Zalka_2025", varietyResponse.source());
    }

    @Test
    void getVarietyById_shouldThrowException_whenIdDoesNotExist() {
        // Arrange
        Long nonExistentId = 99L;
        when(varietyRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        // Act
        VarietyNotFoundException ex = assertThrows(VarietyNotFoundException.class, () -> {
            varietyService.getVarietyById(nonExistentId);
        });
        // Assert
        assertEquals("Variety with ID 99 not found", ex.getMessage());
    }

    @Test
    void getAllVarieties_shouldLeaveCurrentTreeCountNull_whenVarietyHasNoEvent() {
        // Arrange: the Zalka row is a scenario, nothing was planted
        when(varietyRepository.findByOptionalFilters(null, null)).thenReturn(List.of(keittOnBlockA()));
        when(populationEventRepository.findTreeBalances(List.of(1L))).thenReturn(List.of());
        // Act
        List<VarietyResponse> varietyResponses = varietyService.getAllVarieties(null, null);
        // Assert: unknown, not zero, and the declared count is still there
        assertNull(varietyResponses.get(0).currentTreeCount());
        assertEquals(200, varietyResponses.get(0).treeCount());
    }

    @Test
    void getAllVarieties_shouldReturnEventBalance_whenVarietyHasEvents() {
        // Arrange: 150 planted, then the events bring the balance to 138
        when(varietyRepository.findByOptionalFilters(null, "B")).thenReturn(List.of(plantedVariety(2L, "Kent")));
        when(populationEventRepository.findTreeBalances(List.of(2L))).thenReturn(List.of(balance(2L, 138L)));
        // Act
        List<VarietyResponse> varietyResponses = varietyService.getAllVarieties(null, "B");
        // Assert
        assertEquals(150, varietyResponses.get(0).treeCount());
        assertEquals(138, varietyResponses.get(0).currentTreeCount());
    }

    @Test
    void getAllVarieties_shouldComputeEveryBalanceInOneQuery() {
        // Arrange
        when(varietyRepository.findByOptionalFilters(null, null))
                .thenReturn(List.of(keittOnBlockA(), plantedVariety(2L, "Kent")));
        when(populationEventRepository.findTreeBalances(List.of(1L, 2L))).thenReturn(List.of(balance(2L, 150L)));
        // Act
        List<VarietyResponse> varietyResponses = varietyService.getAllVarieties(null, null);
        // Assert
        verify(populationEventRepository, times(1)).findTreeBalances(any());
        assertNull(varietyResponses.get(0).currentTreeCount());
        assertEquals(150, varietyResponses.get(1).currentTreeCount());
    }

    @Test
    void getAllVarieties_shouldNotQueryEvents_whenNoVarietyMatches() {
        // Arrange
        when(varietyRepository.findByOptionalFilters(null, "ZZZ")).thenReturn(List.of());
        // Act
        varietyService.getAllVarieties(null, "ZZZ");
        // Assert
        verify(populationEventRepository, never()).findTreeBalances(any());
    }

    @Test
    void getVarietyById_shouldReturnEventBalance_whenVarietyHasEvents() {
        // Arrange
        when(varietyRepository.findById(2L)).thenReturn(Optional.of(plantedVariety(2L, "Kent")));
        when(populationEventRepository.findTreeBalances(List.of(2L))).thenReturn(List.of(balance(2L, 150L)));
        // Act
        VarietyResponse varietyResponse = varietyService.getVarietyById(2L);
        // Assert
        assertEquals(150, varietyResponse.currentTreeCount());
    }
}
