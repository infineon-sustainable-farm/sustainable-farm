package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerMovementType;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerUnit;

import java.time.Instant;
import java.time.LocalDate;

/**
 * API representation of a movement of a fertilizer's stock: a purchase, an
 * application or a loss.
 *
 * <p>{@code quantity} is in {@code unit}, the unit of the fertilizer, and is
 * always positive: {@code movementType} says whether it entered or left the
 * stock. The components of the other movement types are {@code null}: a
 * purchase has no block, an application has no supplier.
 *
 * <p>{@code totalCost} is in {@code currency}, as entered. {@code totalCostXof}
 * (rounded to the franc) and {@code totalCostEur} (rounded to the cent) are
 * computed on every read with the rate stored in {@code currency_rate}; all four
 * are {@code null} when the purchase has no cost. While no rate is recorded,
 * only the amount in the currency of the purchase is given: {@code totalCostEur}
 * is {@code null} for a cost in XOF, {@code totalCostXof} for a cost in EUR.
 */
public record FertilizerMovementResponse(
        Long id,
        Long fertilizerId,
        String fertilizerName,
        FertilizerMovementType movementType,
        LocalDate movementDate,
        Double quantity,
        FertilizerUnit unit,
        Integer farmId,
        String blockCode,
        String applicator,
        String method,
        String supplier,
        Double totalCost,
        String currency,
        Long totalCostXof,
        Double totalCostEur,
        String reason,
        String source,
        Instant lastUpdated) {
}
