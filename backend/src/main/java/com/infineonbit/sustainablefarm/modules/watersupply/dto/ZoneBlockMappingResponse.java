package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.ZoneBlockMapping;
import java.time.Instant;
import java.util.UUID;

public record ZoneBlockMappingResponse(
        UUID id,
        UUID zoneId,
        String blockCode,
        UUID farmId,
        Instant createdAt) {
    public static ZoneBlockMappingResponse from(ZoneBlockMapping mapping) {
        return new ZoneBlockMappingResponse(mapping.getId(), mapping.getZoneId(), mapping.getBlockCode(),
                mapping.getFarmId(), mapping.getCreatedAt());
    }
}
