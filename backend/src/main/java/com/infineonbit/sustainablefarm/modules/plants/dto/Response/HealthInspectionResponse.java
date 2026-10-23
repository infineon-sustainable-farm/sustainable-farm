package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import com.infineonbit.sustainablefarm.modules.plants.entity.HealthCategory;
import com.infineonbit.sustainablefarm.modules.plants.entity.InspectionMethod;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * API representation of an inspection of a block, with the problems seen.
 *
 * <p>{@code healthCategory} is computed from {@code healthScorePct} on every
 * read. {@code findings} is empty when nothing was found.
 */
public record HealthInspectionResponse(
        Long id,
        Integer farmId,
        String blockCode,
        LocalDate inspectedOn,
        Integer healthScorePct,
        HealthCategory healthCategory,
        String observer,
        InspectionMethod method,
        String source,
        Instant lastUpdated,
        List<HealthFindingResponse> findings) {
}
