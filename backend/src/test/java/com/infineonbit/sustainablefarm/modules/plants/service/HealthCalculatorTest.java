package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.entity.HealthCategory;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthFindingStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HealthCalculatorTest {

    private static final LocalDate RESOLVED_ON = LocalDate.of(2026, 9, 20);

    @Test
    void category_shouldFollowTheFiveBands_atEachBound() {
        assertEquals(HealthCategory.SEVERE_STRESS, HealthCalculator.category(0));
        assertEquals(HealthCategory.SEVERE_STRESS, HealthCalculator.category(20));
        assertEquals(HealthCategory.WEAKENED, HealthCalculator.category(21));
        assertEquals(HealthCategory.WEAKENED, HealthCalculator.category(40));
        assertEquals(HealthCategory.MODERATE, HealthCalculator.category(41));
        assertEquals(HealthCategory.MODERATE, HealthCalculator.category(60));
        assertEquals(HealthCategory.HEALTHY, HealthCalculator.category(61));
        assertEquals(HealthCategory.HEALTHY, HealthCalculator.category(80));
        assertEquals(HealthCategory.VERY_HEALTHY, HealthCalculator.category(81));
        assertEquals(HealthCategory.VERY_HEALTHY, HealthCalculator.category(100));
    }

    @Test
    void category_shouldRefuseAScoreOutsideZeroToHundred() {
        assertThrows(IllegalArgumentException.class, () -> HealthCalculator.category(-1));
        assertThrows(IllegalArgumentException.class, () -> HealthCalculator.category(101));
    }

    @Test
    void status_shouldBeUntreated_whenNotTreatedAndNotResolved() {
        assertEquals(HealthFindingStatus.UNTREATED, HealthCalculator.status(0, null));
    }

    @Test
    void status_shouldBeInProgress_whenTreatedAndNotResolved() {
        assertEquals(HealthFindingStatus.IN_PROGRESS, HealthCalculator.status(1, null));
        assertEquals(HealthFindingStatus.IN_PROGRESS, HealthCalculator.status(3, null));
    }

    @Test
    void status_shouldBeTreated_whenTreatedAndResolved() {
        assertEquals(HealthFindingStatus.TREATED, HealthCalculator.status(2, RESOLVED_ON));
    }

    @Test
    void status_shouldBeClosedWithoutTreatment_whenResolvedWithoutTreatment() {
        assertEquals(HealthFindingStatus.CLOSED_WITHOUT_TREATMENT, HealthCalculator.status(0, RESOLVED_ON));
    }
}
