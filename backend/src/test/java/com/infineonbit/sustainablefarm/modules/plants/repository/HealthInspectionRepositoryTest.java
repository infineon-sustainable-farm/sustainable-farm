package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.HealthInspection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
public class HealthInspectionRepositoryTest {

    @Autowired
    private HealthInspectionRepository healthInspectionRepository;

    private HealthInspection inspection(Integer farmId, String blockCode, LocalDate inspectedOn) {
        HealthInspection inspection = new HealthInspection();
        inspection.setFarmId(farmId);
        inspection.setBlockCode(blockCode);
        inspection.setInspectedOn(inspectedOn);
        inspection.setHealthScorePct(60);
        inspection.setSource("user_entry");
        return healthInspectionRepository.save(inspection);
    }

    private List<Long> find(Integer farmId, String blockCode, LocalDate from, LocalDate to) {
        return healthInspectionRepository.findByOptionalFilters(farmId, blockCode, from, to).stream()
                .map(HealthInspection::getId)
                .toList();
    }

    @Test
    void findByOptionalFilters_shouldOrderByDateThenId() {
        // Arrange: saved out of date order, two on the same day
        HealthInspection late = inspection(null, "C", LocalDate.of(2026, 9, 21));
        HealthInspection early = inspection(null, "D", LocalDate.of(2026, 9, 1));
        HealthInspection sameDay = inspection(null, "C", LocalDate.of(2026, 9, 1));
        // Act & Assert
        assertEquals(List.of(early.getId(), sameDay.getId(), late.getId()), find(null, null, null, null));
    }

    @Test
    void findByOptionalFilters_shouldFilterOnTheBlockAndTheDates_bothIncluded() {
        // Arrange
        HealthInspection september1 = inspection(null, "C", LocalDate.of(2026, 9, 1));
        HealthInspection september10 = inspection(null, "C", LocalDate.of(2026, 9, 10));
        HealthInspection september21 = inspection(null, "C", LocalDate.of(2026, 9, 21));
        HealthInspection otherBlock = inspection(null, "D", LocalDate.of(2026, 9, 10));
        // Act & Assert
        assertEquals(List.of(september1.getId(), september10.getId(), september21.getId()),
                find(null, "C", null, null));
        assertEquals(List.of(september10.getId(), otherBlock.getId()),
                find(null, null, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10)));
        assertEquals(List.of(september10.getId(), september21.getId()),
                find(null, "C", LocalDate.of(2026, 9, 10), null));
        assertEquals(List.of(), find(null, null, LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 1)));
    }

    @Test
    void findByOptionalFilters_shouldTellANullFarmFromFarm1() {
        // Arrange
        HealthInspection noFarm = inspection(null, "C", LocalDate.of(2026, 9, 1));
        HealthInspection farm1 = inspection(1, "C", LocalDate.of(2026, 9, 2));
        // Act & Assert: no filter means every farm; farm 1 leaves out the rows without a farm
        assertEquals(List.of(noFarm.getId(), farm1.getId()), find(null, null, null, null));
        assertEquals(List.of(farm1.getId()), find(1, null, null, null));
        assertEquals(List.of(), find(2, null, null, null));
    }
}
