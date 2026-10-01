package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.Zone;
import java.time.Instant;
import java.util.UUID;

public record ZoneResponse(UUID id, UUID fieldId, String name, Double areaHectares,
                           String irrigationMethod, Instant createdAt) {
    public static ZoneResponse from(Zone zone) {
        return new ZoneResponse(zone.getId(), zone.getFieldId(), zone.getName(), zone.getAreaHectares(),
                zone.getIrrigationMethod(), zone.getCreatedAt());
    }
}