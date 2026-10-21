package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.HarvestRecord;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
public class HarvestRecordRepositoryTest {

    @Autowired
    private VarietyRepository varietyRepository;

    @Autowired
    private HarvestRecordRepository harvestRecordRepository;

    private Variety variety(Integer farmId, String blockCode, String name) {
        Variety variety = new Variety();
        variety.setFarmId(farmId);
        variety.setBlockCode(blockCode);
        variety.setName(name);
        return varietyRepository.save(variety);
    }

    private HarvestRecord harvest(Variety variety, LocalDate date, double quantityKg) {
        return harvest(variety, date, quantityKg, null);
    }

    private HarvestRecord harvest(Variety variety, LocalDate date, double quantityKg, String lotNumber) {
        HarvestRecord harvest = new HarvestRecord();
        harvest.setLotNumber(lotNumber);
        harvest.setVariety(variety);
        harvest.setHarvestDate(date);
        harvest.setQuantityKg(quantityKg);
        harvest.setSource("user_entry");
        return harvestRecordRepository.save(harvest);
    }

    private List<Long> find(Integer farmId, String blockCode, LocalDate from, LocalDate to) {
        return harvestRecordRepository.findByOptionalFilters(farmId, blockCode, from, to).stream()
                .map(HarvestRecord::getId)
                .toList();
    }

    @Test
    void findByOptionalFilters_shouldReturnEveryHarvestByDateThenId_whenNoFilterIsGiven() {
        // Arrange: two pickings on the same day, saved out of date order
        Variety keitt = variety(null, "B", "Keitt");
        HarvestRecord late = harvest(keitt, LocalDate.of(2026, 7, 2), 300.0);
        HarvestRecord firstPicking = harvest(keitt, LocalDate.of(2026, 6, 10), 800.0);
        HarvestRecord secondPicking = harvest(keitt, LocalDate.of(2026, 6, 10), 800.0);
        // Act & Assert
        assertEquals(List.of(firstPicking.getId(), secondPicking.getId(), late.getId()),
                find(null, null, null, null));
    }

    @Test
    void findByOptionalFilters_shouldKeepFarmNullAndFarm1Apart() {
        // Arrange: the same block and variety on no farm and on farm 1
        HarvestRecord noFarm = harvest(variety(null, "B", "Keitt"), LocalDate.of(2026, 6, 10), 800.0);
        HarvestRecord farm1 = harvest(variety(1, "B", "Keitt"), LocalDate.of(2026, 6, 10), 500.0);
        // Act & Assert: farm 1 excludes the row without a farm; no farm filter means every farm
        assertEquals(List.of(farm1.getId()), find(1, null, null, null));
        assertEquals(List.of(noFarm.getId(), farm1.getId()), find(null, null, null, null));
        assertTrue(find(2, null, null, null).isEmpty());
    }

    @Test
    void findByOptionalFilters_shouldFilterOnTheBlockOfTheVariety() {
        // Arrange
        HarvestRecord blockB = harvest(variety(null, "B", "Keitt"), LocalDate.of(2026, 6, 10), 800.0);
        harvest(variety(null, "A", "Kent"), LocalDate.of(2026, 5, 2), 400.0);
        // Act & Assert
        assertEquals(List.of(blockB.getId()), find(null, "B", null, null));
        assertTrue(find(null, "C", null, null).isEmpty());
    }

    @Test
    void findByOptionalFilters_shouldIncludeBothBounds() {
        // Arrange
        Variety keitt = variety(null, "B", "Keitt");
        harvest(keitt, LocalDate.of(2026, 5, 31), 100.0);
        HarvestRecord first = harvest(keitt, LocalDate.of(2026, 6, 1), 200.0);
        HarvestRecord last = harvest(keitt, LocalDate.of(2026, 6, 30), 300.0);
        harvest(keitt, LocalDate.of(2026, 7, 1), 400.0);
        // Act & Assert
        assertEquals(List.of(first.getId(), last.getId()),
                find(null, null, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)));
    }

    @Test
    void findByOptionalFilters_shouldApplyASingleBound() {
        // Arrange
        Variety keitt = variety(null, "B", "Keitt");
        HarvestRecord may = harvest(keitt, LocalDate.of(2026, 5, 31), 100.0);
        HarvestRecord june = harvest(keitt, LocalDate.of(2026, 6, 1), 200.0);
        // Act & Assert
        assertEquals(List.of(june.getId()), find(null, null, LocalDate.of(2026, 6, 1), null));
        assertEquals(List.of(may.getId()), find(null, null, null, LocalDate.of(2026, 5, 31)));
    }

    @Test
    void findByOptionalFilters_shouldReturnNothing_whenFromIsAfterTo() {
        // Arrange
        harvest(variety(null, "B", "Keitt"), LocalDate.of(2026, 6, 10), 800.0);
        // Act & Assert: an empty list, not an error
        assertTrue(find(null, null, LocalDate.of(2026, 6, 30), LocalDate.of(2026, 6, 1)).isEmpty());
    }

    @Test
    void findLotNumbersStartingWith_shouldReturnTheLotsOfOneBlockAndYearOnly() {
        // Arrange: lots of block A in 2026, and of the blocks and years that come close
        harvest(variety(null, "A", "Keitt"), LocalDate.of(2026, 6, 10), 800.0, "LOT-2026-A-001");
        harvest(variety(1, "A", "Kent"), LocalDate.of(2026, 7, 2), 300.0, "LOT-2026-A-002");
        harvest(variety(null, "A1", "Keitt"), LocalDate.of(2026, 6, 10), 100.0, "LOT-2026-A1-001");
        harvest(variety(null, "AB", "Keitt"), LocalDate.of(2026, 6, 10), 100.0, "LOT-2026-AB-001");
        harvest(variety(null, "BA", "Keitt"), LocalDate.of(2026, 6, 10), 100.0, "LOT-2026-BA-001");
        harvest(variety(null, "A", "Amelie"), LocalDate.of(2025, 6, 10), 100.0, "LOT-2025-A-001");
        harvest(variety(null, "A", "Palmer"), LocalDate.of(2026, 5, 2), 100.0);
        // Act
        List<String> lots = harvestRecordRepository.findLotNumbersStartingWith("LOT-2026-A-");
        // Assert: the two lots of block A for 2026, whatever their farm, and no other
        assertEquals(List.of("LOT-2026-A-001", "LOT-2026-A-002"), lots.stream().sorted().toList());
    }

    @Test
    void save_shouldRefuseASecondHarvestWithTheSameLotNumber() {
        // Arrange
        harvest(variety(null, "B", "Keitt"), LocalDate.of(2026, 6, 10), 800.0, "LOT-2026-B-001");
        Variety kent = variety(null, "B", "Kent");
        // Act & Assert: refused by the unique constraint, whatever the variety
        assertThrows(DataIntegrityViolationException.class,
                () -> harvest(kent, LocalDate.of(2026, 6, 10), 500.0, "LOT-2026-B-001"));
    }

    @Test
    void save_shouldAcceptSeveralHarvestsWithoutLotNumber() {
        // Arrange: rows written before lot numbers existed
        Variety keitt = variety(null, "B", "Keitt");
        // Act
        harvest(keitt, LocalDate.of(2026, 6, 10), 800.0);
        harvest(keitt, LocalDate.of(2026, 6, 11), 700.0);
        // Assert: two NULLs are not a duplicate
        assertEquals(2, find(null, "B", null, null).size());
    }
}
