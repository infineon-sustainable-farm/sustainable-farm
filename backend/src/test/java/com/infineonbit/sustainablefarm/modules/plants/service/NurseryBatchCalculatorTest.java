package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryEvent;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryEventType;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryStage;
import com.infineonbit.sustainablefarm.modules.plants.service.NurseryBatchCalculator.BatchSummary;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NurseryBatchCalculatorTest {

    private static final LocalDate MARCH_2 = LocalDate.of(2026, 3, 2);
    private static final LocalDate MARCH_25 = LocalDate.of(2026, 3, 25);
    private static final LocalDate SEPTEMBER_1 = LocalDate.of(2026, 9, 1);
    private static final LocalDate SEPTEMBER_15 = LocalDate.of(2026, 9, 15);
    private static final LocalDate SEPTEMBER_20 = LocalDate.of(2026, 9, 20);

    private static NurseryEvent stageChange(long id, LocalDate date, NurseryStage stage) {
        return new NurseryEvent(id, null, NurseryEventType.STAGE_CHANGE, date, stage, null, null, null, null,
                "user_entry", null);
    }

    private static NurseryEvent loss(long id, LocalDate date, int quantity) {
        return new NurseryEvent(id, null, NurseryEventType.LOSS, date, null, quantity, "Graft failure", null, null,
                "user_entry", null);
    }

    private static NurseryEvent transplant(long id, LocalDate date, int quantity) {
        return new NurseryEvent(id, null, NurseryEventType.TRANSPLANT, date, null, quantity, null, "E", null,
                "user_entry", null);
    }

    @Test
    void summarize_shouldKeepEveryPlant_whenOnlyTheInitialStageIsRecorded() {
        // Act
        BatchSummary summary = NurseryBatchCalculator.summarize(150,
                List.of(stageChange(1, MARCH_2, NurseryStage.GERMINATION)));
        // Assert
        assertEquals(new BatchSummary(0, 0, 150, 150, new BigDecimal("100.0"), NurseryStage.GERMINATION, MARCH_2),
                summary);
    }

    @Test
    void summarize_shouldTakeTheLossesFromTheCurrentAndSurvivedCounts() {
        // Act
        BatchSummary summary = NurseryBatchCalculator.summarize(150, List.of(
                stageChange(1, MARCH_2, NurseryStage.GERMINATION),
                loss(2, MARCH_25, 12),
                loss(3, SEPTEMBER_15, 3)));
        // Assert
        assertEquals(15, summary.lossCount());
        assertEquals(0, summary.transplantedCount());
        assertEquals(135, summary.currentCount());
        assertEquals(135, summary.survivedCount());
        assertEquals(new BigDecimal("90.0"), summary.survivalRatePct());
    }

    @Test
    void summarize_shouldCountTheTransplantedPlantsAsSurvivors() {
        // Act: the mockup batch, 138 of 150 left, then 100 transplanted
        BatchSummary summary = NurseryBatchCalculator.summarize(150, List.of(
                stageChange(1, MARCH_2, NurseryStage.GERMINATION),
                loss(2, SEPTEMBER_15, 12),
                transplant(3, SEPTEMBER_20, 100)));
        // Assert
        assertEquals(12, summary.lossCount());
        assertEquals(100, summary.transplantedCount());
        assertEquals(38, summary.currentCount());
        assertEquals(138, summary.survivedCount());
        assertEquals(new BigDecimal("92.0"), summary.survivalRatePct());
    }

    @Test
    void summarize_shouldReachZero_whenEveryPlantIsLostOrTransplanted() {
        // Act
        BatchSummary summary = NurseryBatchCalculator.summarize(150, List.of(
                stageChange(1, MARCH_2, NurseryStage.GERMINATION),
                loss(2, SEPTEMBER_15, 12),
                transplant(3, SEPTEMBER_20, 100),
                transplant(4, SEPTEMBER_20, 38)));
        // Assert: the transplanted plants still count as survivors
        assertEquals(0, summary.currentCount());
        assertEquals(138, summary.transplantedCount());
        assertEquals(138, summary.survivedCount());
        assertEquals(new BigDecimal("92.0"), summary.survivalRatePct());
    }

    @Test
    void survivalRatePct_shouldRoundHalfUpToOneDecimal() {
        assertEquals(new BigDecimal("66.7"), NurseryBatchCalculator.survivalRatePct(2, 3));
        assertEquals(new BigDecimal("16.7"), NurseryBatchCalculator.survivalRatePct(1, 6));
        // 6.25 is a half: rounded up, not to the even 6.2
        assertEquals(new BigDecimal("6.3"), NurseryBatchCalculator.survivalRatePct(1, 16));
        assertEquals(new BigDecimal("62.5"), NurseryBatchCalculator.survivalRatePct(5, 8));
        assertEquals(new BigDecimal("0.0"), NurseryBatchCalculator.survivalRatePct(0, 5));
        assertEquals(new BigDecimal("100.0"), NurseryBatchCalculator.survivalRatePct(150, 150));
    }

    @Test
    void summarize_shouldTakeTheLatestStageChangeByDate_notTheLastRecorded() {
        // Act: the change of September 1 was recorded before the one of March 25
        BatchSummary summary = NurseryBatchCalculator.summarize(150, List.of(
                stageChange(1, MARCH_2, NurseryStage.GERMINATION),
                stageChange(2, SEPTEMBER_1, NurseryStage.GRAFTED),
                stageChange(3, MARCH_25, NurseryStage.ROOTSTOCK_GROWTH)));
        // Assert
        assertEquals(NurseryStage.GRAFTED, summary.currentStage());
        assertEquals(SEPTEMBER_1, summary.currentStageSince());
    }

    @Test
    void summarize_shouldBreakADateTieByTheHighestIdentifier() {
        // Act: two changes on the same day, listed out of identifier order
        BatchSummary summary = NurseryBatchCalculator.summarize(150, List.of(
                stageChange(5, SEPTEMBER_1, NurseryStage.HARDENING),
                stageChange(4, SEPTEMBER_1, NurseryStage.GRAFTED),
                stageChange(1, MARCH_2, NurseryStage.GERMINATION)));
        // Assert
        assertEquals(NurseryStage.HARDENING, summary.currentStage());
        assertEquals(SEPTEMBER_1, summary.currentStageSince());
    }

    @Test
    void summarize_shouldFollowABackwardStage() {
        // Act: the graft did not take, the batch is back to its rootstock
        BatchSummary summary = NurseryBatchCalculator.summarize(150, List.of(
                stageChange(1, MARCH_2, NurseryStage.GERMINATION),
                stageChange(2, MARCH_25, NurseryStage.ROOTSTOCK_GROWTH),
                stageChange(3, SEPTEMBER_1, NurseryStage.GRAFTED),
                stageChange(4, SEPTEMBER_15, NurseryStage.ROOTSTOCK_GROWTH)));
        // Assert
        assertEquals(NurseryStage.ROOTSTOCK_GROWTH, summary.currentStage());
        assertEquals(SEPTEMBER_15, summary.currentStageSince());
    }

    @Test
    void notEnoughPlantsMessage_shouldReadLikeTheFertilizerMessage() {
        assertEquals("Not enough plants in batch P1: 138 left, 200 requested",
                NurseryBatchCalculator.notEnoughPlantsMessage("P1", 138, 200));
    }
}
