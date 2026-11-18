package com.infineonbit.sustainablefarm.modules.plants.api;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A recorded harvest, as the other modules see it.
 *
 * @param id          identifier of the harvest record, the link the other modules keep
 * @param farmId      farm of the variety row the harvest belongs to
 * @param blockCode   block of that variety row, as stored, for example {@code "A"}
 * @param varietyId   identifier of that variety row
 * @param varietyName planted name of the variety, as stored, for example {@code "Keitt"}
 * @param harvestDate date of the harvest
 * @param quantityKg  harvested quantity, in kg
 * @param source      origin of the record, for example {@code "user_entry"}
 * @param lastUpdated last write time of the record
 */
public record HarvestSummary(
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
