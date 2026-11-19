package com.infineonbit.sustainablefarm.modules.farm.api;

import java.time.Instant;

/**
 * Read view of a zone, with its block and farm.
 *
 * @param blockCode code of the zone's block, or {@code null} when the block has none
 */
public record ZoneDto(Long id, Long blockId, Long farmId, String blockCode, String name, Double areaHectares,
                      String irrigationMethod, Double cropCoefficient, Integer emitterCount,
                      Double emitterNominalFlowLh, Instant createdAt) {

    /** Theoretical flow of the zone's network (L/h), or null when the network is not described. */
    public Double theoreticalFlowLitersPerHour() {
        if (emitterCount == null || emitterCount <= 0 || emitterNominalFlowLh == null || emitterNominalFlowLh <= 0) {
            return null;
        }
        return emitterCount * emitterNominalFlowLh;
    }
}
