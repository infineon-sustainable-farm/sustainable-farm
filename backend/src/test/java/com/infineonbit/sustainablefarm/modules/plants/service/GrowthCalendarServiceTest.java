package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.GrowthCalendarResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.GrowthCalendar;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import com.infineonbit.sustainablefarm.modules.plants.exception.GrowthCalendarNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.GrowthCalendarRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GrowthCalendarServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 16);

    @Mock
    private GrowthCalendarRepository growthCalendarRepository;

    @Mock
    private VarietyRepository varietyRepository;

    @InjectMocks
    private GrowthCalendarService growthCalendarService;

    /** The seeded Zalka 2025 row: no planting date, so no age and no phase. */
    private static GrowthCalendar zalkaBlockA() {
        return new GrowthCalendar(
                1L, null, "A", null, "rainy season (year unknown)",
                null, null, null, "Zalka_2025", null);
    }

    private static GrowthCalendar blockWithPlantingDate(String block, LocalDate plantingDate) {
        return new GrowthCalendar(
                2L, null, block, plantingDate, null,
                null, null, null, "TEST", null);
    }

    private static Variety variety(String name, String block, Integer farmId) {
        Variety variety = new Variety();
        variety.setName(name);
        variety.setBlockCode(block);
        variety.setFarmId(farmId);
        return variety;
    }

    @Test
    void getAllGrowthCalendarEntries_shouldReturnMatchingEntries_whenFilterMatches() {
        // Arrange
        when(growthCalendarRepository.findByOptionalFilters(null, "A")).thenReturn(List.of(zalkaBlockA()));
        when(varietyRepository.findByOptionalFilters(null, "A")).thenReturn(List.of(variety("Keitt", "A", null)));
        // Act
        List<GrowthCalendarResponse> responses = growthCalendarService.getAllGrowthCalendarEntries(null, "A", TODAY);
        // Assert
        assertEquals(1, responses.size());
        assertEquals("A", responses.get(0).blockCode());
        assertEquals("rainy season (year unknown)", responses.get(0).datePrecision());
        assertEquals("Zalka_2025", responses.get(0).source());
    }

    @Test
    void getAllGrowthCalendarEntries_shouldReturnEmptyList_whenFilterMatchesNothing() {
        // Arrange
        when(growthCalendarRepository.findByOptionalFilters(null, "ZZZ")).thenReturn(List.of());
        // Act
        List<GrowthCalendarResponse> responses = growthCalendarService.getAllGrowthCalendarEntries(null, "ZZZ", TODAY);
        // Assert
        assertTrue(responses.isEmpty());
        verifyNoInteractions(varietyRepository);
    }

    @Test
    void getGrowthCalendarEntryById_shouldReturnEntry_whenIdExists() {
        // Arrange
        when(growthCalendarRepository.findById(1L)).thenReturn(Optional.of(zalkaBlockA()));
        when(varietyRepository.findByOptionalFilters(null, "A")).thenReturn(List.of(variety("Keitt", "A", null)));
        // Act
        GrowthCalendarResponse response = growthCalendarService.getGrowthCalendarEntryById(1L, TODAY);
        // Assert
        assertEquals(1L, response.id());
        assertEquals(List.of("Keitt"), response.varieties());
    }

    @Test
    void getGrowthCalendarEntryById_shouldThrowException_whenIdDoesNotExist() {
        // Arrange
        Long nonExistentId = 999L;
        when(growthCalendarRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        // Act
        GrowthCalendarNotFoundException ex = assertThrows(GrowthCalendarNotFoundException.class, () -> {
            growthCalendarService.getGrowthCalendarEntryById(nonExistentId, TODAY);
        });
        // Assert
        assertEquals("Growth calendar entry with ID 999 not found", ex.getMessage());
    }

    @Test
    void getAllGrowthCalendarEntries_shouldLeaveAgeAndPhaseNull_whenPlantingDateIsNull() {
        // Arrange
        when(growthCalendarRepository.findByOptionalFilters(null, null)).thenReturn(List.of(zalkaBlockA()));
        when(varietyRepository.findByOptionalFilters(null, null)).thenReturn(List.of());
        // Act
        GrowthCalendarResponse response = growthCalendarService.getAllGrowthCalendarEntries(null, null, TODAY).get(0);
        // Assert
        assertNull(response.plantingDate());
        assertNull(response.ageYears());
        assertNull(response.ageMonths());
        assertNull(response.growthPhase());
        assertNull(response.phaseYearsBand());
        assertNull(response.currentStage());
        assertNull(response.localRainfallMm());
    }

    @Test
    void getAllGrowthCalendarEntries_shouldComputeAgeAndPhase_whenPlantingDateIsKnown() {
        // Arrange: planted 4 years and 6 months before TODAY
        when(growthCalendarRepository.findByOptionalFilters(null, "B"))
                .thenReturn(List.of(blockWithPlantingDate("B", LocalDate.of(2022, 3, 16))));
        when(varietyRepository.findByOptionalFilters(null, "B")).thenReturn(List.of());
        // Act
        GrowthCalendarResponse response = growthCalendarService.getAllGrowthCalendarEntries(null, "B", TODAY).get(0);
        // Assert
        assertEquals(4, response.ageYears());
        assertEquals(6, response.ageMonths());
        assertEquals("gradual production", response.growthPhase());
        assertEquals("3–5 yrs", response.phaseYearsBand());
    }

    @Test
    void getAllGrowthCalendarEntries_shouldListEveryVariety_whenBlockHasSeveral() {
        // Arrange
        when(growthCalendarRepository.findByOptionalFilters(null, "A")).thenReturn(List.of(zalkaBlockA()));
        when(varietyRepository.findByOptionalFilters(null, "A")).thenReturn(List.of(
                variety("Keitt", "A", null),
                variety("Kent", "A", null),
                variety("Keitt", "A", null)));
        // Act
        GrowthCalendarResponse response = growthCalendarService.getAllGrowthCalendarEntries(null, "A", TODAY).get(0);
        // Assert
        assertEquals(List.of("Keitt", "Kent"), response.varieties());
    }

    @Test
    void getAllGrowthCalendarEntries_shouldReturnNoVariety_whenBlockHasNone() {
        // Arrange
        when(growthCalendarRepository.findByOptionalFilters(null, "B"))
                .thenReturn(List.of(blockWithPlantingDate("B", null)));
        when(varietyRepository.findByOptionalFilters(null, "B")).thenReturn(List.of());
        // Act
        GrowthCalendarResponse response = growthCalendarService.getAllGrowthCalendarEntries(null, "B", TODAY).get(0);
        // Assert
        assertTrue(response.varieties().isEmpty());
    }

    @Test
    void getAllGrowthCalendarEntries_shouldIgnoreVarietiesOfAnotherFarm_whenBlockNameIsShared() {
        // Arrange: entry has no farm; a block "A" variety of farm 7 must not be attached
        when(growthCalendarRepository.findByOptionalFilters(null, "A")).thenReturn(List.of(zalkaBlockA()));
        when(varietyRepository.findByOptionalFilters(null, "A")).thenReturn(List.of(
                variety("Keitt", "A", null),
                variety("Amelie", "A", 7)));
        // Act
        GrowthCalendarResponse response = growthCalendarService.getAllGrowthCalendarEntries(null, "A", TODAY).get(0);
        // Assert
        assertEquals(List.of("Keitt"), response.varieties());
    }
}
