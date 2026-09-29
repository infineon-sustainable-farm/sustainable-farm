package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerType;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerUnit;

import java.time.Instant;

/**
 * API representation of a fertilizer of the catalogue, with its stock.
 *
 * <p>{@code currentStock} is computed from the recorded movements on every read,
 * in {@code unit}: purchases minus applications and losses. It is 0 for a
 * fertilizer that has no movement. {@code belowThreshold} is {@code true} when a
 * {@code reorderThreshold} exists and the stock is at or below it, and
 * {@code false} otherwise, including when there is no threshold.
 */
public record FertilizerResponse(
        Long id,
        String name,
        FertilizerType fertilizerType,
        String composition,
        FertilizerUnit unit,
        Double reorderThreshold,
        Double currentStock,
        boolean belowThreshold,
        String source,
        Instant lastUpdated) {
}
