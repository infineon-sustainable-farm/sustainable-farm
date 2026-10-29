package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.Zone;
import java.time.Instant;
import java.util.UUID;

/**
 * API view of a zone. The network fields (emitters) let the client know whether
 * clogging detection is usable, and {@code theoreticalFlowLh} avoids recomputing the calculation.
 */
public record ZoneResponse(UUID id, UUID fieldId, String name, Double areaHectares,
                           String irrigationMethod, Double cropCoefficient, Integer emitterCount,
                           Double emitterNominalFlowLh, Double theoreticalFlowLh, Instant createdAt) {
    public static ZoneResponse from(Zone zone) {
        return new ZoneResponse(zone.getId(), zone.getFieldId(), zone.getName(), zone.getAreaHectares(),
                zone.getIrrigationMethod(), zone.getCropCoefficient(), zone.getEmitterCount(),
                zone.getEmitterNominalFlowLh(), zone.theoreticalFlowLitersPerHour(), zone.getCreatedAt());
    }
}