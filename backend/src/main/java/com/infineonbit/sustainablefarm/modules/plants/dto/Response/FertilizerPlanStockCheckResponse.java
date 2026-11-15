package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerUnit;

import java.time.LocalDate;
import java.util.List;

/**
 * The fertilizers whose stock does not cover the applications planned in the
 * coming days.
 *
 * @param days     how many days ahead the check looks
 * @param today    the day the check was computed for
 * @param until    last planned date counted, included: {@code today} plus {@code days}
 * @param warnings one line per fertilizer short of stock, by name; empty when
 *                 every stock covers its plans
 */
public record FertilizerPlanStockCheckResponse(
        Integer days,
        LocalDate today,
        LocalDate until,
        List<Warning> warnings) {

    /**
     * A fertilizer whose planned quantity exceeds its stock. Quantities are in
     * {@code unit}, the unit of the fertilizer.
     *
     * @param fertilizerId    the fertilizer identifier
     * @param fertilizerName  the fertilizer name
     * @param unit            the unit of the fertilizer
     * @param plannedQuantity total of its applications still PLANNED up to {@code until},
     *                        late ones included
     * @param overdueQuantity the part of {@code plannedQuantity} whose date has passed
     * @param currentStock    its stock today
     * @param shortfall       {@code plannedQuantity} minus {@code currentStock}
     */
    public record Warning(
            Long fertilizerId,
            String fertilizerName,
            FertilizerUnit unit,
            Double plannedQuantity,
            Double overdueQuantity,
            Double currentStock,
            Double shortfall) {
    }
}
