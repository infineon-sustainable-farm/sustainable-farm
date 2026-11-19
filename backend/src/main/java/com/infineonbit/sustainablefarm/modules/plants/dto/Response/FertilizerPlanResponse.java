package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerPlanStatus;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerUnit;

import java.time.Instant;
import java.time.LocalDate;

/**
 * API representation of a planned application of a fertilizer on a block.
 *
 * <p>{@code plannedQuantity} is in {@code unit}, the unit of the fertilizer.
 *
 * <p>{@code daysFromToday} is {@code plannedDate} minus today, in days:
 * positive before the date, 0 on it, negative once it has passed. It is given
 * while the plan is PLANNED and {@code null} afterwards. {@code overdue} is
 * true for a PLANNED application whose date has passed. Both are computed on
 * every read and never stored.
 *
 * <p>The costs are computed on every read at the last purchase price: the cost
 * of the most recent purchase of the fertilizer that has one, divided by its
 * quantity. {@code unitPriceXof} and {@code unitPriceEur} are that price for
 * one unit, {@code priceDate} the date of that purchase, and
 * {@code plannedCostXof} and {@code plannedCostEur} the planned quantity at
 * that price. Amounts in FCFA are rounded to the franc, amounts in euros to
 * the cent; the planned cost is computed from the exact price, not from the
 * rounded one. All five are {@code null} while no purchase of the fertilizer
 * has a cost. While no EUR to XOF rate is recorded, only the amounts in the
 * currency of that purchase are given.
 *
 * <p>{@code movementId}, {@code actualDate} and {@code actualQuantity} describe
 * the real application once the plan is DONE, and are {@code null} otherwise.
 */
public record FertilizerPlanResponse(
        Long id,
        Long fertilizerId,
        String fertilizerName,
        FertilizerUnit unit,
        Long farmId,
        String blockCode,
        LocalDate plannedDate,
        Double plannedQuantity,
        FertilizerPlanStatus status,
        boolean overdue,
        Integer daysFromToday,
        Long unitPriceXof,
        Double unitPriceEur,
        LocalDate priceDate,
        Long plannedCostXof,
        Double plannedCostEur,
        Long movementId,
        LocalDate actualDate,
        Double actualQuantity,
        String note,
        String source,
        Instant lastUpdated) {
}
