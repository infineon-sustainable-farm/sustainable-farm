package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.HarvestRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HarvestResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.HarvestRecord;
import com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEvent;
import com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEventType;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import com.infineonbit.sustainablefarm.modules.plants.exception.HarvestNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.HarvestRecordRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HarvestServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-24T10:00:00Z");
    private static final LocalDate PLANTING_DATE = LocalDate.of(2023, 9, 24);

    @Mock
    private VarietyRepository varietyRepository;

    @Mock
    private PopulationEventRepository populationEventRepository;

    @Mock
    private HarvestRecordRepository harvestRecordRepository;

    @InjectMocks
    private HarvestService harvestService;

    private static HarvestRequest request(Integer farmId, String blockCode, String varietyName,
                                          LocalDate harvestDate, double quantityKg) {
        return new HarvestRequest(farmId, blockCode, varietyName, harvestDate, quantityKg);
    }

    private static Variety variety(Long id, Integer farmId, String blockCode, String name) {
        Variety variety = new Variety();
        variety.setId(id);
        variety.setFarmId(farmId);
        variety.setBlockCode(blockCode);
        variety.setName(name);
        return variety;
    }

    private static PopulationEvent planting(Variety variety, LocalDate date) {
        return new PopulationEvent(50L, variety, PopulationEventType.PLANTING, date, 150, "user_entry", NOW);
    }

    /** Keitt planted on block B on 2023-09-24, as in the orchard of the runtime check. */
    private Variety keittPlantedOnB() {
        Variety keitt = variety(10L, null, "B", "Keitt");
        when(varietyRepository.findByFarmBlockAndName(null, "B", "Keitt")).thenReturn(List.of(keitt));
        when(populationEventRepository.findFirstByVarietyIdAndEventTypeOrderByEventDateAsc(
                10L, PopulationEventType.PLANTING)).thenReturn(Optional.of(planting(keitt, PLANTING_DATE)));
        return keitt;
    }

    /** Saving the harvest gives it an identifier, as the database would. */
    private void harvestSaveAssignsId(long id) {
        when(harvestRecordRepository.save(any(HarvestRecord.class))).thenAnswer(invocation -> {
            HarvestRecord harvest = invocation.getArgument(0);
            harvest.setId(id);
            return harvest;
        });
    }

    @Test
    void recordHarvest_shouldStoreTheHarvest_whenVarietyIsPlanted() {
        // Arrange
        Variety keitt = keittPlantedOnB();
        harvestSaveAssignsId(200L);
        // Act
        HarvestResponse response = harvestService.recordHarvest(
                request(null, "B", "Keitt", LocalDate.of(2026, 6, 10), 800.0), NOW);
        // Assert: the stored harvest
        ArgumentCaptor<HarvestRecord> captor = ArgumentCaptor.forClass(HarvestRecord.class);
        verify(harvestRecordRepository).save(captor.capture());
        HarvestRecord harvest = captor.getValue();
        assertSame(keitt, harvest.getVariety());
        assertEquals(LocalDate.of(2026, 6, 10), harvest.getHarvestDate());
        assertEquals(800.0, harvest.getQuantityKg());
        assertEquals("user_entry", harvest.getSource());
        assertEquals(NOW, harvest.getLastUpdated());
        // Assert: the first lot of block B for 2026, read from the lots already given
        assertEquals("LOT-2026-B-001", harvest.getLotNumber());
        verify(harvestRecordRepository).findLotNumbersStartingWith("LOT-2026-B-");
        // Assert: the response describes the variety row
        assertEquals(new HarvestResponse(200L, "LOT-2026-B-001", null, "B", 10L, "Keitt", LocalDate.of(2026, 6, 10),
                800.0, "user_entry", NOW), response);
    }

    @Test
    void recordHarvest_shouldNormalizeBlockAndName_likeAPlanting() {
        // Arrange: " b " and " keitt " find the row of "Keitt" on block B
        Variety keitt = variety(10L, null, "B", "Keitt");
        when(varietyRepository.findByFarmBlockAndName(null, "B", "keitt")).thenReturn(List.of(keitt));
        when(populationEventRepository.findFirstByVarietyIdAndEventTypeOrderByEventDateAsc(
                10L, PopulationEventType.PLANTING)).thenReturn(Optional.of(planting(keitt, PLANTING_DATE)));
        harvestSaveAssignsId(200L);
        // Act
        HarvestResponse response = harvestService.recordHarvest(
                request(null, " b ", " keitt ", LocalDate.of(2026, 6, 10), 800.0), NOW);
        // Assert: the stored spelling comes back, and the lot number uses it
        assertEquals("B", response.blockCode());
        assertEquals("Keitt", response.varietyName());
        assertEquals("LOT-2026-B-001", response.lotNumber());
    }

    @Test
    void recordHarvest_shouldGiveOneMoreThanTheHighestNumberOfTheBlockAndYear() {
        // Arrange: lots 001 and 003 of block B for 2026, 002 missing
        keittPlantedOnB();
        harvestSaveAssignsId(200L);
        when(harvestRecordRepository.findLotNumbersStartingWith("LOT-2026-B-"))
                .thenReturn(List.of("LOT-2026-B-003", "LOT-2026-B-001"));
        // Act
        HarvestResponse response = harvestService.recordHarvest(
                request(null, "B", "Keitt", LocalDate.of(2026, 6, 10), 800.0), NOW);
        // Assert: after the highest, never a number already given
        assertEquals("LOT-2026-B-004", response.lotNumber());
    }

    @Test
    void recordHarvest_shouldNumberByTheYearOfTheHarvestDate_notOfTheDayItIsRecorded() {
        // Arrange: a picking of 31 December 2025, recorded in September 2026
        keittPlantedOnB();
        harvestSaveAssignsId(200L);
        when(harvestRecordRepository.findLotNumbersStartingWith("LOT-2025-B-")).thenReturn(List.of("LOT-2025-B-007"));
        // Act
        HarvestResponse response = harvestService.recordHarvest(
                request(null, "B", "Keitt", LocalDate.of(2025, 12, 31), 300.0), NOW);
        // Assert: a lot of 2025, after the last one of that year
        assertEquals("LOT-2025-B-008", response.lotNumber());
        verify(harvestRecordRepository, never()).findLotNumbersStartingWith("LOT-2026-B-");
    }

    @Test
    void recordHarvest_shouldWriteAtLeastThreeDigits_andGoOnPast999() {
        // Arrange
        keittPlantedOnB();
        harvestSaveAssignsId(200L);
        when(harvestRecordRepository.findLotNumbersStartingWith("LOT-2026-B-")).thenReturn(List.of("LOT-2026-B-999"));
        // Act
        HarvestResponse response = harvestService.recordHarvest(
                request(null, "B", "Keitt", LocalDate.of(2026, 6, 10), 800.0), NOW);
        // Assert: the sequence grows a digit instead of wrapping around
        assertEquals("LOT-2026-B-1000", response.lotNumber());
    }

    @Test
    void recordHarvest_shouldAcceptAHarvestOnThePlantingDay() {
        // Arrange
        keittPlantedOnB();
        harvestSaveAssignsId(200L);
        // Act & Assert: the planting date itself is not "before" the planting
        assertEquals(PLANTING_DATE, harvestService.recordHarvest(
                request(null, "B", "Keitt", PLANTING_DATE, 1.5), NOW).harvestDate());
    }

    @Test
    void recordHarvest_shouldAcceptTwoHarvestsOnTheSameDay() {
        // Arrange: two pickings
        keittPlantedOnB();
        harvestSaveAssignsId(200L);
        HarvestRequest picking = request(null, "B", "Keitt", LocalDate.of(2026, 6, 10), 800.0);
        // Act
        harvestService.recordHarvest(picking, NOW);
        harvestService.recordHarvest(picking, NOW);
        // Assert: both stored, no duplicate rule
        verify(harvestRecordRepository, times(2)).save(any(HarvestRecord.class));
    }

    @Test
    void recordHarvest_shouldThrow422_whenVarietyIsNotOnTheBlock() {
        // Arrange: no Kent row on block C
        when(varietyRepository.findByFarmBlockAndName(null, "C", "Kent")).thenReturn(List.of());
        // Act
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> harvestService.recordHarvest(
                request(null, " c ", "Kent", LocalDate.of(2026, 6, 10), 800.0), NOW));
        // Assert: the name as sent, the block as normalized; nothing written
        assertEquals("No planting of Kent is recorded on block C", ex.getMessage());
        verifyNoInteractions(populationEventRepository);
        verify(harvestRecordRepository, never()).save(any());
    }

    @Test
    void recordHarvest_shouldThrow422_whenVarietyRowHasNoPlanting() {
        // Arrange: the Zalka row of the dev profile, which has no PLANTING event
        Variety zalkaKeitt = variety(1L, null, "A", "Keitt");
        when(varietyRepository.findByFarmBlockAndName(null, "A", "keitt")).thenReturn(List.of(zalkaKeitt));
        when(populationEventRepository.findFirstByVarietyIdAndEventTypeOrderByEventDateAsc(
                1L, PopulationEventType.PLANTING)).thenReturn(Optional.empty());
        // Act
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> harvestService.recordHarvest(
                request(null, "A", "keitt", LocalDate.of(2026, 6, 10), 800.0), NOW));
        // Assert: the stored name
        assertEquals("No planting of Keitt is recorded on block A", ex.getMessage());
        verify(harvestRecordRepository, never()).save(any());
    }

    @Test
    void recordHarvest_shouldThrow422_whenHarvestIsBeforeThePlanting() {
        // Arrange
        keittPlantedOnB();
        // Act
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> harvestService.recordHarvest(
                request(null, "B", "Keitt", LocalDate.of(2023, 1, 1), 800.0), NOW));
        // Assert
        assertEquals("The harvest date 2023-01-01 is before the planting date 2023-09-24 of Keitt on block B",
                ex.getMessage());
        verify(harvestRecordRepository, never()).save(any());
    }

    @Test
    void recordHarvest_shouldLookUpTheFarmAsSent_soNoFarmNeverFindsFarm1() {
        // Arrange: Keitt is planted on block B of farm 1 only; the NULL-safe lookup
        // (see VarietyRepositoryTest) finds no row without a farm
        when(varietyRepository.findByFarmBlockAndName(null, "B", "Keitt")).thenReturn(List.of());
        // Act
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> harvestService.recordHarvest(
                request(null, "B", "Keitt", LocalDate.of(2026, 6, 10), 800.0), NOW));
        // Assert: the farm is passed as NULL, not dropped
        assertEquals("No planting of Keitt is recorded on block B", ex.getMessage());
        verify(varietyRepository).findByFarmBlockAndName(null, "B", "Keitt");
    }

    @Test
    void getAllHarvests_shouldPassFiltersAndTurnABlankBlockOrLotNumberIntoNoFilter() {
        // Arrange
        Variety keitt = variety(10L, 1, "B", "Keitt");
        HarvestRecord harvest = new HarvestRecord(200L, "LOT-2026-B-001", keitt, LocalDate.of(2026, 6, 10), 800.0,
                "user_entry", NOW);
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);
        when(harvestRecordRepository.findByOptionalFilters(1, null, null, from, to)).thenReturn(List.of(harvest));
        // Act
        List<HarvestResponse> responses = harvestService.getAllHarvests(1, "  ", " ", from, to);
        // Assert
        assertEquals(List.of(new HarvestResponse(200L, "LOT-2026-B-001", 1, "B", 10L, "Keitt",
                LocalDate.of(2026, 6, 10), 800.0, "user_entry", NOW)), responses);
    }

    @Test
    void getAllHarvests_shouldTrimAndUpperCaseTheLotNumber() {
        // Arrange
        Variety keitt = variety(10L, 1, "B", "Keitt");
        HarvestRecord harvest = new HarvestRecord(200L, "LOT-2026-B-001", keitt, LocalDate.of(2026, 6, 10), 800.0,
                "user_entry", NOW);
        when(harvestRecordRepository.findByOptionalFilters(null, null, "LOT-2026-B-001", null, null))
                .thenReturn(List.of(harvest));
        // Act
        List<HarvestResponse> responses = harvestService.getAllHarvests(null, null, " lot-2026-b-001 ", null, null);
        // Assert: the lot number as stored is looked up, as an exact value
        assertEquals(List.of(200L), responses.stream().map(HarvestResponse::id).toList());
    }

    @Test
    void getAllHarvests_shouldReturnAnEmptyList_forAnUnknownLotNumber() {
        // Arrange: no harvest holds this number
        when(harvestRecordRepository.findByOptionalFilters(null, null, "LOT-1999-Z-999", null, null))
                .thenReturn(List.of());
        // Act & Assert: an empty list, not an error
        assertEquals(List.of(), harvestService.getAllHarvests(null, null, "LOT-1999-Z-999", null, null));
    }

    @Test
    void getAllHarvests_withoutLotNumber_shouldReadEveryLotOfTheFarmBlockAndPeriod() {
        // Arrange: the form the harvest status and the yield comparison use
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);
        when(harvestRecordRepository.findByOptionalFilters(1, "B", null, from, to)).thenReturn(List.of());
        // Act
        harvestService.getAllHarvests(1, "B", from, to);
        // Assert: no lot number filter
        verify(harvestRecordRepository).findByOptionalFilters(1, "B", null, from, to);
    }

    @Test
    void getHarvestById_shouldReturnEveryField_withTheFarmBlockAndNameOfTheVarietyRow() {
        // Arrange
        Variety keitt = variety(10L, 1, "B", "Keitt");
        HarvestRecord harvest = new HarvestRecord(200L, "LOT-2026-B-001", keitt, LocalDate.of(2026, 6, 10), 800.0,
                "user_entry", NOW);
        when(harvestRecordRepository.findWithVarietyById(200L)).thenReturn(Optional.of(harvest));
        // Act
        HarvestResponse response = harvestService.getHarvestById(200L);
        // Assert
        assertEquals(new HarvestResponse(200L, "LOT-2026-B-001", 1, "B", 10L, "Keitt", LocalDate.of(2026, 6, 10),
                800.0, "user_entry", NOW), response);
    }

    @Test
    void getHarvestById_shouldGiveNoLotNumber_forAHarvestRecordedBeforeLotNumbers() {
        // Arrange: a row written before the lot number existed
        Variety keitt = variety(10L, 1, "B", "Keitt");
        HarvestRecord harvest = new HarvestRecord(150L, null, keitt, LocalDate.of(2026, 5, 2), 300.0,
                "user_entry", NOW);
        when(harvestRecordRepository.findWithVarietyById(150L)).thenReturn(Optional.of(harvest));
        // Act & Assert: null, never a number made up on the way out
        assertNull(harvestService.getHarvestById(150L).lotNumber());
    }

    @Test
    void getHarvestById_shouldThrowNotFound_whenTheHarvestIsUnknown() {
        // Arrange
        when(harvestRecordRepository.findWithVarietyById(999L)).thenReturn(Optional.empty());
        // Act
        HarvestNotFoundException exception = assertThrows(HarvestNotFoundException.class,
                () -> harvestService.getHarvestById(999L));
        // Assert
        assertEquals("Harvest with ID 999 not found", exception.getMessage());
    }
}
