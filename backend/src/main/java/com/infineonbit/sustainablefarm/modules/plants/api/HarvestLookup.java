package com.infineonbit.sustainablefarm.modules.plants.api;

import java.util.List;
import java.util.Optional;

/**
 * Read access to the harvests recorded in plants, for the other modules
 * (docs/adr/0003-communication-entre-modules.md).
 *
 * <p>Gives the same harvests as {@code GET /api/plants/harvests}, read in the
 * same process instead of over HTTP.
 */
public interface HarvestLookup {

    /**
     * Lists the harvests, optionally of one farm and one block.
     *
     * @param farmId    farm identifier, or {@code null} for every farm
     * @param blockCode block code as stored, for example {@code "A"}, or
     *                  {@code null} or blank for every block
     * @return the matching harvests, ordered by date then identifier; possibly empty
     */
    List<HarvestSummary> listHarvests(Integer farmId, String blockCode);

    /**
     * Finds one harvest.
     *
     * @param id the harvest identifier
     * @return the harvest, or empty when no harvest has this identifier
     */
    Optional<HarvestSummary> findHarvest(Long id);
}
