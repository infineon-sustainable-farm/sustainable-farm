package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import com.infineonbit.sustainablefarm.modules.plants.entity.HealthCategory;

import java.time.LocalDate;

/**
 * Current health of a block: the score of its most recent inspection, with the
 * date and the category of that score.
 *
 * <p>The most recent inspection is the latest by date, then by identifier when
 * two share a date. Computed on every read, never stored.
 */
public record BlockHealthResponse(
        Integer farmId,
        String blockCode,
        Long inspectionId,
        LocalDate inspectedOn,
        Integer healthScorePct,
        HealthCategory healthCategory) {
}
