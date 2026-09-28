package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.entity.HealthCategory;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthFindingStatus;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthInspection;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthTreatment;
import com.infineonbit.sustainablefarm.modules.plants.service.HealthCalculator.TreatmentSummary;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HealthCalculatorTest {

    private static final LocalDate RESOLVED_ON = LocalDate.of(2026, 9, 20);

    private static HealthTreatment treatment(LocalDate treatedOn, int preHarvestIntervalDays) {
        HealthTreatment treatment = new HealthTreatment();
        treatment.setTreatedOn(treatedOn);
        treatment.setPreHarvestIntervalDays(preHarvestIntervalDays);
        return treatment;
    }

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

    @Test
    void harvestAllowedFrom_shouldAddThePreHarvestIntervalToTheTreatmentDate() {
        assertEquals(LocalDate.of(2026, 9, 18), HealthCalculator.harvestAllowedFrom(LocalDate.of(2026, 9, 4), 14));
        assertEquals(LocalDate.of(2026, 9, 23), HealthCalculator.harvestAllowedFrom(LocalDate.of(2026, 9, 23), 0));
        // Across a month and a year
        assertEquals(LocalDate.of(2027, 1, 3), HealthCalculator.harvestAllowedFrom(LocalDate.of(2026, 12, 20), 14));
    }

    @Test
    void summarize_shouldCountTheTreatments_andKeepTheLatestDates() {
        // Arrange: the earlier treatment has the longer interval
        List<HealthTreatment> treatments = List.of(
                treatment(LocalDate.of(2026, 9, 4), 14),
                treatment(LocalDate.of(2026, 9, 10), 1));
        // Act
        TreatmentSummary summary = HealthCalculator.summarize(treatments);
        // Assert: the harvest date is the latest any treatment allows, not the one of the latest treatment
        assertEquals(new TreatmentSummary(2, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 18)), summary);
    }

    @Test
    void summarize_shouldBeEmpty_whenThereIsNoTreatment() {
        assertEquals(new TreatmentSummary(0, null, null), HealthCalculator.summarize(List.of()));
    }

    private static HealthInspection inspection(long id, Integer farmId, String blockCode, LocalDate inspectedOn) {
        return new HealthInspection(id, farmId, blockCode, inspectedOn, 60, null, null, "user_entry", null);
    }

    private static List<Long> ids(List<HealthInspection> inspections) {
        return inspections.stream().map(HealthInspection::getId).toList();
    }

    @Test
    void latestByBlock_shouldKeepTheLatestInspectionOfEachBlock_byDateThenId() {
        // Arrange: block C inspected three times, twice on its latest day; block D once
        List<HealthInspection> inspections = List.of(
                inspection(4, null, "C", LocalDate.of(2026, 9, 21)),
                inspection(1, null, "C", LocalDate.of(2026, 9, 1)),
                inspection(5, null, "D", LocalDate.of(2026, 9, 2)),
                inspection(6, null, "C", LocalDate.of(2026, 9, 21)));
        // Act & Assert: the highest identifier breaks the tie of the same date
        assertEquals(List.of(6L, 5L), ids(HealthCalculator.latestByBlock(inspections)));
        assertEquals(List.of(), HealthCalculator.latestByBlock(List.of()));
    }

    @Test
    void latestByBlock_shouldTellANullFarmFromFarm1_andSortByBlockThenFarm() {
        // Arrange: block C with no farm and block C of farm 1 are two blocks
        List<HealthInspection> inspections = List.of(
                inspection(1, 1, "C", LocalDate.of(2026, 9, 1)),
                inspection(2, null, "D", LocalDate.of(2026, 9, 2)),
                inspection(3, null, "C", LocalDate.of(2026, 9, 3)),
                inspection(4, 1, "C", LocalDate.of(2026, 8, 1)));
        // Act & Assert: by block code, then farm with no farm first
        assertEquals(List.of(3L, 1L, 2L), ids(HealthCalculator.latestByBlock(inspections)));
    }
}
