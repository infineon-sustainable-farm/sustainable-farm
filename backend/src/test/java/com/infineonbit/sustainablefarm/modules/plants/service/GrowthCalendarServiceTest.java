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
    void obtainAllGrowthCalendarEntries_shouldReturnMatchingEntries_whenFilterMatches() {
        // Arrange
        when(growthCalendarRepository.findByOptionalFilters(null, "A")).thenReturn(List.of(zalkaBlockA()));
        when(varietyRepository.findByOptionalFilters(null, "A")).thenReturn(List.of(variety("Keitt", "A", null)));
        // Act
        List<GrowthCalendarResponse> responses = growthCalendarService.obtainAllGrowthCalendarEntries(null, "A", TODAY);
        // Assert
        assertEquals(1, responses.size());
        assertEquals("A", responses.get(0).bloc_parcelle());
        assertEquals("rainy season (year unknown)", responses.get(0).precision_date());
        assertEquals("Zalka_2025", responses.get(0).source());
    }

    @Test
    void obtainAllGrowthCalendarEntries_shouldReturnEmptyList_whenFilterMatchesNothing() {
        // Arrange
        when(growthCalendarRepository.findByOptionalFilters(null, "ZZZ")).thenReturn(List.of());
        // Act
        List<GrowthCalendarResponse> responses = growthCalendarService.obtainAllGrowthCalendarEntries(null, "ZZZ", TODAY);
        // Assert
        assertTrue(responses.isEmpty());
        verifyNoInteractions(varietyRepository);
    }

    @Test
    void obtainGrowthCalendarEntryById_shouldReturnEntry_whenIdExists() {
        // Arrange
        when(growthCalendarRepository.findById(1L)).thenReturn(Optional.of(zalkaBlockA()));
        when(varietyRepository.findByOptionalFilters(null, "A")).thenReturn(List.of(variety("Keitt", "A", null)));
        // Act
        GrowthCalendarResponse response = growthCalendarService.obtainGrowthCalendarEntryById(1L, TODAY);
        // Assert
        assertEquals(1L, response.id());
        assertEquals(List.of("Keitt"), response.varietes());
    }

    @Test
    void obtainGrowthCalendarEntryById_shouldThrowException_whenIdDoesNotExist() {
        // Arrange
        Long nonExistentId = 999L;
        when(growthCalendarRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        // Act
        GrowthCalendarNotFoundException ex = assertThrows(GrowthCalendarNotFoundException.class, () -> {
            growthCalendarService.obtainGrowthCalendarEntryById(nonExistentId, TODAY);
        });
        // Assert
        assertEquals("Growth calendar entry with ID 999 not found", ex.getMessage());
    }

    @Test
    void obtainAllGrowthCalendarEntries_shouldLeaveAgeAndPhaseNull_whenPlantingDateIsNull() {
        // Arrange
        when(growthCalendarRepository.findByOptionalFilters(null, null)).thenReturn(List.of(zalkaBlockA()));
        when(varietyRepository.findByOptionalFilters(null, null)).thenReturn(List.of());
        // Act
        GrowthCalendarResponse response = growthCalendarService.obtainAllGrowthCalendarEntries(null, null, TODAY).get(0);
        // Assert
        assertNull(response.date_plantation());
        assertNull(response.age_annees());
        assertNull(response.age_mois());
        assertNull(response.phase_croissance());
        assertNull(response.phase_tranche_annees());
        assertNull(response.stade_actuel());
        assertNull(response.pluviometrie_locale_mm());
    }

    @Test
    void obtainAllGrowthCalendarEntries_shouldComputeAgeAndPhase_whenPlantingDateIsKnown() {
        // Arrange: planted 4 years and 6 months before TODAY
        when(growthCalendarRepository.findByOptionalFilters(null, "B"))
                .thenReturn(List.of(blockWithPlantingDate("B", LocalDate.of(2022, 3, 16))));
        when(varietyRepository.findByOptionalFilters(null, "B")).thenReturn(List.of());
        // Act
        GrowthCalendarResponse response = growthCalendarService.obtainAllGrowthCalendarEntries(null, "B", TODAY).get(0);
        // Assert
        assertEquals(4, response.age_annees());
        assertEquals(6, response.age_mois());
        assertEquals("gradual production", response.phase_croissance());
        assertEquals("3–5 yrs", response.phase_tranche_annees());
    }

    @Test
    void obtainAllGrowthCalendarEntries_shouldListEveryVariety_whenBlockHasSeveral() {
        // Arrange
        when(growthCalendarRepository.findByOptionalFilters(null, "A")).thenReturn(List.of(zalkaBlockA()));
        when(varietyRepository.findByOptionalFilters(null, "A")).thenReturn(List.of(
                variety("Keitt", "A", null),
                variety("Kent", "A", null),
                variety("Keitt", "A", null)));
        // Act
        GrowthCalendarResponse response = growthCalendarService.obtainAllGrowthCalendarEntries(null, "A", TODAY).get(0);
        // Assert
        assertEquals(List.of("Keitt", "Kent"), response.varietes());
    }

    @Test
    void obtainAllGrowthCalendarEntries_shouldReturnNoVariety_whenBlockHasNone() {
        // Arrange
        when(growthCalendarRepository.findByOptionalFilters(null, "B"))
                .thenReturn(List.of(blockWithPlantingDate("B", null)));
        when(varietyRepository.findByOptionalFilters(null, "B")).thenReturn(List.of());
        // Act
        GrowthCalendarResponse response = growthCalendarService.obtainAllGrowthCalendarEntries(null, "B", TODAY).get(0);
        // Assert
        assertTrue(response.varietes().isEmpty());
    }

    @Test
    void obtainAllGrowthCalendarEntries_shouldIgnoreVarietiesOfAnotherFarm_whenBlockNameIsShared() {
        // Arrange: entry has no farm; a block "A" variety of farm 7 must not be attached
        when(growthCalendarRepository.findByOptionalFilters(null, "A")).thenReturn(List.of(zalkaBlockA()));
        when(varietyRepository.findByOptionalFilters(null, "A")).thenReturn(List.of(
                variety("Keitt", "A", null),
                variety("Amelie", "A", 7)));
        // Act
        GrowthCalendarResponse response = growthCalendarService.obtainAllGrowthCalendarEntries(null, "A", TODAY).get(0);
        // Assert
        assertEquals(List.of("Keitt"), response.varietes());
    }
}
