package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import java.time.Instant;
import java.time.LocalDate;

/**
 * API representation of a recorded planting.
 *
 * <p>{@code id} is the identifier of the planting event. {@code farmId},
 * {@code blockCode}, {@code varietyId} and {@code varietyName} describe the
 * variety row the planting belongs to, as stored: an existing row keeps its own
 * spelling, so "keitt" planted on the block of "Keitt" comes back as "Keitt".
 */
public record PlantingResponse(
        Long id,
        Integer farmId,
        String blockCode,
        Long varietyId,
        String varietyName,
        LocalDate plantingDate,
        Integer treeCount,
        String source,
        Instant lastUpdated) {
}
