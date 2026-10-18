package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.HarvestRecord;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        HarvestRecord harvest = new HarvestRecord();
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
}
