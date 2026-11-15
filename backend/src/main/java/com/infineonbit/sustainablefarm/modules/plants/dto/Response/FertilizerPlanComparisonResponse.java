package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerUnit;

import java.time.LocalDate;
import java.util.List;

/**
 * Planned and applied quantities of fertilizer over a period, by block and
 * fertilizer.
 *
 * @param from first day of the period, included, or {@code null} for no lower bound
 * @param to   last day of the period, included, or {@code null} for no upper bound
 * @param rows one line per block and fertilizer that has a plan or an
 *             application in the period, by block then fertilizer name
 */
public record FertilizerPlanComparisonResponse(
        LocalDate from,
        LocalDate to,
        List<Row> rows) {

    /**
     * One block and one fertilizer over the period. Quantities are in
     * {@code unit}, the unit of the fertilizer.
     *
     * @param blockCode         the block, as stored
     * @param fertilizerId      the fertilizer identifier
     * @param fertilizerName    the fertilizer name
     * @param unit              the unit of the fertilizer
     * @param plannedQuantity   total of the PLANNED and DONE plans whose planned date is in the
     *                          period; cancelled plans are left out
     * @param remainingQuantity the part of {@code plannedQuantity} still PLANNED
     * @param appliedQuantity   total really applied: the real applications of the DONE plans
     *                          counted in {@code plannedQuantity}, whatever their own date,
     *                          plus the applications made without a plan in the period
     * @param unplannedQuantity the part of {@code appliedQuantity} applied without a plan
     * @param gapQuantity       {@code appliedQuantity} minus {@code plannedQuantity}: negative
     *                          when less was applied than planned
     * @param gapPercent        {@code gapQuantity} as a percentage of {@code plannedQuantity},
     *                          with one decimal, or {@code null} when nothing was planned
     * @param plannedCostXof    {@code plannedQuantity} at the last purchase price, in FCFA
     * @param plannedCostEur    {@code plannedQuantity} at the last purchase price, in euros
     * @param appliedCostXof    {@code appliedQuantity} at the last purchase price, in FCFA
     * @param appliedCostEur    {@code appliedQuantity} at the last purchase price, in euros
     * @param priceDate         date of the purchase that gives the price, or {@code null}
     *                          when no purchase of the fertilizer has a cost; the four
     *                          costs are then {@code null} too
     */
    public record Row(
            String blockCode,
            Long fertilizerId,
            String fertilizerName,
            FertilizerUnit unit,
            Double plannedQuantity,
            Double remainingQuantity,
            Double appliedQuantity,
            Double unplannedQuantity,
            Double gapQuantity,
            Double gapPercent,
            Long plannedCostXof,
            Double plannedCostEur,
            Long appliedCostXof,
            Double appliedCostEur,
            LocalDate priceDate) {
    }
}
