package com.infineonbit.sustainablefarm.modules.farm.dto;

import com.infineonbit.sustainablefarm.modules.farm.api.ZoneDto;
import java.time.Instant;

/**
 * API view of a zone. {@code fieldId} is the id of the zone's block. The network fields (emitters)
 * let the client know whether clogging detection is usable, and {@code theoreticalFlowLh} avoids
 * recomputing the calculation.
 */
public record ZoneResponse(Long id, Long fieldId, String name, Double areaHectares,
                           String irrigationMethod, Double cropCoefficient, Integer emitterCount,
                           Double emitterNominalFlowLh, Double theoreticalFlowLh, Instant createdAt) {
    public static ZoneResponse from(ZoneDto zone) {
        return new ZoneResponse(zone.id(), zone.blockId(), zone.name(), zone.areaHectares(),
                zone.irrigationMethod(), zone.cropCoefficient(), zone.emitterCount(),
                zone.emitterNominalFlowLh(), zone.theoreticalFlowLitersPerHour(), zone.createdAt());
    }
}
