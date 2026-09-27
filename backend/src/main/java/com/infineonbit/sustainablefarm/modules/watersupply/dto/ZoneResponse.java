package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.Zone;
import java.time.Instant;
import java.util.UUID;

/**
 * Vue API d'une zone. Les champs de reseau (goutteurs) permettent au client de savoir si la
 * detection de colmatage est exploitable, et {@code theoreticalFlowLh} evite de refaire le calcul.
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