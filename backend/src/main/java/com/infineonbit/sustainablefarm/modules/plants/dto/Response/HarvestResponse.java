package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import java.time.Instant;
import java.time.LocalDate;

/**
 * API representation of a recorded harvest.
 *
 * <p>{@code id} is the identifier of the harvest record, the link the other
 * modules keep. {@code lotNumber} is the lot number given when the harvest was
 * recorded (PL-05-05), for example "LOT-2026-A-002": the second lot given to
 * block A for a harvest of 2026, counted in the order the harvests were
 * recorded. It is unique and never changes; it is null for a harvest recorded
 * before lot numbers existed, unless the startup filling of the dev profile
 * numbered it.
 *
 * <p>{@code farmId}, {@code blockCode}, {@code varietyId} and
 * {@code varietyName} describe the variety row the harvest belongs to, as
 * stored: a harvest of "keitt" comes back as "Keitt" when that is the planted
 * name.
 */
public record HarvestResponse(
        Long id,
        String lotNumber,
        Integer farmId,
        String blockCode,
        Long varietyId,
        String varietyName,
        LocalDate harvestDate,
        Double quantityKg,
        String source,
        Instant lastUpdated) {
}
