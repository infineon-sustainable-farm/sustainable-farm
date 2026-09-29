package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PlantingRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantingResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.GrowthCalendar;
import com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEvent;
import com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEventType;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import com.infineonbit.sustainablefarm.modules.plants.repository.GrowthCalendarRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.PopulationEventRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PlantingServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-24T10:00:00Z");
    private static final Instant EARLIER = Instant.parse("2025-01-01T00:00:00Z");
    private static final LocalDate PLANTING_DATE = LocalDate.of(2023, 9, 24);

    @Mock
    private VarietyRepository varietyRepository;

    @Mock
    private GrowthCalendarRepository growthCalendarRepository;

    @Mock
    private PopulationEventRepository populationEventRepository;

    @InjectMocks
    private PlantingService plantingService;

    private static PlantingRequest request(Integer farmId, String blockCode, String varietyName,
                                           LocalDate plantingDate, int treeCount) {
        return new PlantingRequest(farmId, blockCode, varietyName, plantingDate, treeCount);
    }

    /** The seeded Zalka 2025 variety row, as the dev profile loads it. */
    private static Variety zalkaKeitt() {
        return new Variety(
                1L, null, "Keitt", 200, 8.0, 8.0, null, 44000.0,
                null, null, "A", null, "Zalka_2025", null, null);
    }

    private static GrowthCalendar calendar(String block, LocalDate plantingDate, String precision, String source) {
        return new GrowthCalendar(
                5L, null, block, plantingDate, precision,
                null, null, null, source, EARLIER, null);
    }

    /** Saving a new variety row gives it an identifier, as the database would. */
    private void varietySaveAssignsId(long id) {
        when(varietyRepository.save(any(Variety.class))).thenAnswer(invocation -> {
            Variety variety = invocation.getArgument(0);
            variety.setId(id);
            return variety;
        });
    }

    /** Saving the planting event gives it an identifier, as the database would. */
    private void eventSaveAssignsId(long id) {
        when(populationEventRepository.save(any(PopulationEvent.class))).thenAnswer(invocation -> {
            PopulationEvent event = invocation.getArgument(0);
            event.setId(id);
            return event;
        });
    }

    private PopulationEvent savedEvent() {
        ArgumentCaptor<PopulationEvent> captor = ArgumentCaptor.forClass(PopulationEvent.class);
        verify(populationEventRepository).save(captor.capture());
        return captor.getValue();
    }

    private GrowthCalendar savedCalendar() {
        ArgumentCaptor<GrowthCalendar> captor = ArgumentCaptor.forClass(GrowthCalendar.class);
        verify(growthCalendarRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void recordPlanting_shouldCreateVarietyCalendarAndEvent_whenBlockIsNew() {
        // Arrange
        when(varietyRepository.findByFarmBlockAndNameForUpdate(null, "B", "Keitt")).thenReturn(List.of());
        when(growthCalendarRepository.findByFarmAndBlockForUpdate(null, "B")).thenReturn(List.of());
        varietySaveAssignsId(10L);
        eventSaveAssignsId(100L);
        // Act
        PlantingResponse response = plantingService.recordPlanting(
                request(null, "B", "Keitt", PLANTING_DATE, 150), NOW);
        // Assert: the variety row, with only what a planting says
        ArgumentCaptor<Variety> varietyCaptor = ArgumentCaptor.forClass(Variety.class);
        verify(varietyRepository).save(varietyCaptor.capture());
        Variety variety = varietyCaptor.getValue();
        assertNull(variety.getFarmId());
        assertEquals("B", variety.getBlockCode());
        assertEquals("Keitt", variety.getName());
        assertEquals(150, variety.getTreeCount());
        assertEquals("user_entry", variety.getSource());
        assertEquals(NOW, variety.getLastUpdated());
        assertNull(variety.getRowSpacingM());
        assertNull(variety.getTreeSpacingM());
        assertNull(variety.getTreeDensityPerHa());
        assertNull(variety.getExpectedYieldKg());
        assertNull(variety.getActualYieldKg());
        assertNull(variety.getVigor());
        assertNull(variety.getPlantOrigin());
        // Assert: the calendar row of the block
        GrowthCalendar entry = savedCalendar();
        assertNull(entry.getFarmId());
        assertEquals("B", entry.getBlockCode());
        assertEquals(PLANTING_DATE, entry.getPlantingDate());
        assertEquals("day known", entry.getDatePrecision());
        assertEquals("user_entry", entry.getSource());
        assertEquals(NOW, entry.getLastUpdated());
        assertNull(entry.getCurrentStage());
        assertNull(entry.getPhaseYears());
        assertNull(entry.getLocalRainfallMm());
        // Assert: the event and the response
        PopulationEvent event = savedEvent();
        assertSame(variety, event.getVariety());
        assertEquals(PopulationEventType.PLANTING, event.getEventType());
        assertEquals(PLANTING_DATE, event.getEventDate());
        assertEquals(150, event.getTreeCount());
        assertEquals("user_entry", event.getSource());
        assertEquals(NOW, event.getLastUpdated());
        assertEquals(new PlantingResponse(100L, null, "B", 10L, "Keitt", PLANTING_DATE, 150, "user_entry", NOW),
                response);
    }

    @Test
    void recordPlanting_shouldReuseExistingVariety_whenNameDiffersOnlyByCase() {
        // Arrange: the repository matches names ignoring case, so "keitt" finds the Zalka row
        Variety zalkaKeitt = zalkaKeitt();
        when(varietyRepository.findByFarmBlockAndNameForUpdate(null, "A", "keitt")).thenReturn(List.of(zalkaKeitt));
        when(populationEventRepository.existsByVarietyIdAndEventType(1L, PopulationEventType.PLANTING))
                .thenReturn(false);
        when(growthCalendarRepository.findByFarmAndBlockForUpdate(null, "A")).thenReturn(List.of());
        eventSaveAssignsId(100L);
        // Act
        PlantingResponse response = plantingService.recordPlanting(
                request(null, "A", "keitt", PLANTING_DATE, 150), NOW);
        // Assert: no second row, and the existing one is not modified
        verify(varietyRepository, never()).save(any(Variety.class));
        assertEquals(200, zalkaKeitt.getTreeCount());
        assertEquals("Keitt", zalkaKeitt.getName());
        assertEquals("Zalka_2025", zalkaKeitt.getSource());
        assertNull(zalkaKeitt.getLastUpdated());
        assertSame(zalkaKeitt, savedEvent().getVariety());
        assertEquals(1L, response.varietyId());
        assertEquals("Keitt", response.varietyName());
        assertEquals(150, response.treeCount());
    }

    @Test
    void recordPlanting_shouldThrowConflict_whenVarietyIsAlreadyPlanted() {
        // Arrange
        when(varietyRepository.findByFarmBlockAndNameForUpdate(null, "A", "Keitt")).thenReturn(List.of(zalkaKeitt()));
        when(populationEventRepository.existsByVarietyIdAndEventType(1L, PopulationEventType.PLANTING))
                .thenReturn(true);
        // Act
        ConflictException ex = assertThrows(ConflictException.class,
                () -> plantingService.recordPlanting(request(null, "A", "Keitt", PLANTING_DATE, 150), NOW));
        // Assert: the message, and nothing written
        assertEquals("A planting of Keitt is already recorded on block A", ex.getMessage());
        verify(varietyRepository, never()).save(any(Variety.class));
        verify(growthCalendarRepository, never()).save(any(GrowthCalendar.class));
        verify(populationEventRepository, never()).save(any(PopulationEvent.class));
    }

    @Test
    void recordPlanting_shouldMoveCalendarDateBack_whenPlantingIsOlder() {
        // Arrange: the block is dated 2024-05-10, the new planting is older
        GrowthCalendar blockA = calendar("A", LocalDate.of(2024, 5, 10), "day known", "user_entry");
        when(varietyRepository.findByFarmBlockAndNameForUpdate(null, "A", "Kent")).thenReturn(List.of());
        when(growthCalendarRepository.findByFarmAndBlockForUpdate(null, "A")).thenReturn(List.of(blockA));
        varietySaveAssignsId(11L);
        eventSaveAssignsId(101L);
        // Act
        plantingService.recordPlanting(request(null, "A", "Kent", LocalDate.of(2022, 3, 1), 40), NOW);
        // Assert
        assertSame(blockA, savedCalendar());
        assertEquals(LocalDate.of(2022, 3, 1), blockA.getPlantingDate());
        assertEquals("day known", blockA.getDatePrecision());
        assertEquals("user_entry", blockA.getSource());
        assertEquals(NOW, blockA.getLastUpdated());
    }

    @Test
    void recordPlanting_shouldLeaveCalendarUntouched_whenRecordedDateIsOlder() {
        // Arrange: the block is dated 2022-03-01, the new planting is later
        GrowthCalendar blockA = calendar("A", LocalDate.of(2022, 3, 1), "day known", "user_entry");
        when(varietyRepository.findByFarmBlockAndNameForUpdate(null, "A", "Kent")).thenReturn(List.of());
        when(growthCalendarRepository.findByFarmAndBlockForUpdate(null, "A")).thenReturn(List.of(blockA));
        varietySaveAssignsId(11L);
        eventSaveAssignsId(101L);
        // Act
        plantingService.recordPlanting(request(null, "A", "Kent", LocalDate.of(2024, 5, 10), 40), NOW);
        // Assert
        verify(growthCalendarRepository, never()).save(any(GrowthCalendar.class));
        assertEquals(LocalDate.of(2022, 3, 1), blockA.getPlantingDate());
        assertEquals(EARLIER, blockA.getLastUpdated());
    }

    @Test
    void recordPlanting_shouldLeaveCalendarUntouched_whenRecordedDateIsTheSame() {
        // Arrange
        GrowthCalendar blockA = calendar("A", PLANTING_DATE, "day known", "user_entry");
        when(varietyRepository.findByFarmBlockAndNameForUpdate(null, "A", "Kent")).thenReturn(List.of());
        when(growthCalendarRepository.findByFarmAndBlockForUpdate(null, "A")).thenReturn(List.of(blockA));
        varietySaveAssignsId(11L);
        eventSaveAssignsId(101L);
        // Act
        plantingService.recordPlanting(request(null, "A", "Kent", PLANTING_DATE, 40), NOW);
        // Assert
        verify(growthCalendarRepository, never()).save(any(GrowthCalendar.class));
        assertEquals(EARLIER, blockA.getLastUpdated());
    }

    @Test
    void recordPlanting_shouldDateCalendar_whenRecordedDateIsNull() {
        // Arrange: the Zalka calendar row has no date; precision and source follow the entered date
        GrowthCalendar zalkaBlockA = calendar("A", null, "rainy season (year unknown)", "Zalka_2025");
        when(varietyRepository.findByFarmBlockAndNameForUpdate(null, "A", "Keitt")).thenReturn(List.of(zalkaKeitt()));
        when(populationEventRepository.existsByVarietyIdAndEventType(1L, PopulationEventType.PLANTING))
                .thenReturn(false);
        when(growthCalendarRepository.findByFarmAndBlockForUpdate(null, "A")).thenReturn(List.of(zalkaBlockA));
        eventSaveAssignsId(100L);
        // Act
        plantingService.recordPlanting(request(null, "A", "Keitt", PLANTING_DATE, 150), NOW);
        // Assert
        assertSame(zalkaBlockA, savedCalendar());
        assertEquals(PLANTING_DATE, zalkaBlockA.getPlantingDate());
        assertEquals("day known", zalkaBlockA.getDatePrecision());
        assertEquals("user_entry", zalkaBlockA.getSource());
        assertEquals(NOW, zalkaBlockA.getLastUpdated());
    }

    @Test
    void recordPlanting_shouldNormalizeBlockCodeAndVarietyName() {
        // Arrange
        when(varietyRepository.findByFarmBlockAndNameForUpdate(null, "A", "Keitt")).thenReturn(List.of());
        when(growthCalendarRepository.findByFarmAndBlockForUpdate(null, "A")).thenReturn(List.of());
        varietySaveAssignsId(10L);
        eventSaveAssignsId(100L);
        // Act
        PlantingResponse response = plantingService.recordPlanting(
                request(null, " a ", "  Keitt  ", PLANTING_DATE, 150), NOW);
        // Assert
        assertEquals("A", response.blockCode());
        assertEquals("Keitt", response.varietyName());
        assertEquals("A", savedCalendar().getBlockCode());
    }

    @Test
    void recordPlanting_shouldKeepTheFarmOfTheRequest_whenFarmIsGiven() {
        // Arrange: farm 1 is looked up as farm 1, never as "any farm"
        when(varietyRepository.findByFarmBlockAndNameForUpdate(1, "A", "Keitt")).thenReturn(List.of());
        when(growthCalendarRepository.findByFarmAndBlockForUpdate(1, "A")).thenReturn(List.of());
        varietySaveAssignsId(12L);
        eventSaveAssignsId(102L);
        // Act
        PlantingResponse response = plantingService.recordPlanting(
                request(1, "A", "Keitt", PLANTING_DATE, 80), NOW);
        // Assert
        assertEquals(1, response.farmId());
        assertEquals(1, savedCalendar().getFarmId());
    }
}
