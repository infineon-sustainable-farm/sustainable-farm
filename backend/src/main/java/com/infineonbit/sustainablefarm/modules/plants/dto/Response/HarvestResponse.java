package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import java.time.Instant;
import java.time.LocalDate;

/**
 * API representation of a recorded harvest.
 *
 * <p>{@code id} is the identifier of the harvest record. {@code farmId},
 * {@code blockCode}, {@code varietyId} and {@code varietyName} describe the
 * variety row the harvest belongs to, as stored: a harvest of "keitt" comes
 * back as "Keitt" when that is the planted name.
 */
public record HarvestResponse(
        Long id,
        Integer farmId,
        String blockCode,
        Long varietyId,
        String varietyName,
        LocalDate harvestDate,
        Double quantityKg,
        String source,
        Instant lastUpdated) {
}
