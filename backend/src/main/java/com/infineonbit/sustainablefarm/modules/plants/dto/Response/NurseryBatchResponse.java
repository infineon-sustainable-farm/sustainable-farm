package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryOrigin;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryStage;

import java.time.Instant;
import java.time.LocalDate;

/**
 * API representation of a nursery batch, with the values computed from its
 * events on every read.
 *
 * <ul>
 *     <li>{@code lossCount} and {@code transplantedCount} are the sums of the
 *         losses and of the transplants.</li>
 *     <li>{@code currentCount} is what is left in the nursery:
 *         {@code initialCount - lossCount - transplantedCount}.</li>
 *     <li>{@code survivedCount} is {@code initialCount - lossCount}: a
 *         transplanted plant survived the nursery.</li>
 *     <li>{@code survivalRatePct} is {@code survivedCount} over
 *         {@code initialCount}, in percent, rounded half up to one decimal.</li>
 *     <li>{@code currentStage} and {@code currentStageSince} come from the
 *         latest stage change, by date then identifier.</li>
 * </ul>
 */
public record NurseryBatchResponse(
        Long id,
        Integer farmId,
        String batchCode,
        String varietyName,
        NurseryOrigin origin,
        String supplier,
        String supplierLotNumber,
        LocalDate startedOn,
        Integer initialCount,
        LocalDate plannedTransplantOn,
        String plannedBlockCode,
        NurseryStage currentStage,
        LocalDate currentStageSince,
        Integer lossCount,
        Integer transplantedCount,
        Integer currentCount,
        Integer survivedCount,
        Double survivalRatePct,
        String source,
        Instant lastUpdated) {
}
