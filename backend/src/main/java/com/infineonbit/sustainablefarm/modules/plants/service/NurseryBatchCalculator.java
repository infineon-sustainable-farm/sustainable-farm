package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryEvent;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryEventType;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryStage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Values of a nursery batch computed on every read from its events, with no
 * data of their own: the counts, the survival rate and the current stage. None
 * of them is stored, as the stock of a fertilizer is not.
 */
final class NurseryBatchCalculator {

    /**
     * What the events of one batch add up to.
     *
     * @param lossCount         plants lost, the sum of the losses
     * @param transplantedCount plants transplanted, the sum of the transplants
     * @param currentCount      plants still in the nursery
     * @param survivedCount     plants that survived the nursery, the transplanted ones included
     * @param survivalRatePct   {@code survivedCount} over the initial count, in percent, with one decimal
     * @param currentStage      stage of the latest stage change, {@code null} without any
     * @param currentStageSince date of that stage change, {@code null} without any
     */
    record BatchSummary(int lossCount, int transplantedCount, int currentCount, int survivedCount,
                        BigDecimal survivalRatePct, NurseryStage currentStage, LocalDate currentStageSince) {
    }

    /** Oldest first: the earliest date, then the lowest identifier among the same date. */
    private static final Comparator<NurseryEvent> CHRONOLOGICAL =
            Comparator.comparing(NurseryEvent::getEventDate).thenComparing(NurseryEvent::getId);

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private NurseryBatchCalculator() {
    }

    /** The plants taken out by the events of one type, 0 when there is none. */
    private static int total(List<NurseryEvent> events, NurseryEventType type) {
        return events.stream()
                .filter(event -> event.getEventType() == type)
                .mapToInt(NurseryEvent::getQuantity)
                .sum();
    }

    /**
     * What the events of one batch add up to. The current stage is that of the
     * latest stage change by date, then by identifier among the same date, not
     * that of the last one recorded.
     *
     * @param initialCount the plants the batch started with
     * @param events       the events of the batch, in any order
     * @return its counts, survival rate and current stage
     */
    static BatchSummary summarize(int initialCount, List<NurseryEvent> events) {
        int lossCount = total(events, NurseryEventType.LOSS);
        int transplantedCount = total(events, NurseryEventType.TRANSPLANT);
        int survivedCount = initialCount - lossCount;
        NurseryEvent latestStageChange = events.stream()
                .filter(event -> event.getEventType() == NurseryEventType.STAGE_CHANGE)
                .max(CHRONOLOGICAL)
                .orElse(null);
        return new BatchSummary(
                lossCount,
                transplantedCount,
                survivedCount - transplantedCount,
                survivedCount,
                survivalRatePct(survivedCount, initialCount),
                latestStageChange == null ? null : latestStageChange.getStage(),
                latestStageChange == null ? null : latestStageChange.getEventDate());
    }

    /**
     * The survival rate of a batch, computed on exact decimals.
     *
     * @param survivedCount plants that survived the nursery
     * @param initialCount  plants the batch started with, 1 or more
     * @return the rate in percent, rounded half up to one decimal: 138 of 150 is 92.0
     */
    static BigDecimal survivalRatePct(int survivedCount, int initialCount) {
        return BigDecimal.valueOf(survivedCount)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(initialCount), 1, RoundingMode.HALF_UP);
    }

    /**
     * Refusal of a loss or a transplant larger than what is left, worded like
     * the fertilizer one: "Not enough plants in batch P1: 138 left, 200 requested".
     *
     * @param batchCode    the batch code
     * @param currentCount plants still in the nursery
     * @param requested    plants of the refused event
     * @return the message of the 422
     */
    static String notEnoughPlantsMessage(String batchCode, int currentCount, int requested) {
        return "Not enough plants in batch " + batchCode + ": " + currentCount + " left, " + requested + " requested";
    }
}
