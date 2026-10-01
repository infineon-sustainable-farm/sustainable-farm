package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterSource;
import java.time.Instant;
import java.util.UUID;

public record WaterSourceResponse(UUID id, UUID farmId, String name, String type,
                                  Double capacityLiters, Double currentLevelLiters,
                                  Double latitude, Double longitude, Instant createdAt) {
    public static WaterSourceResponse from(WaterSource source) {
        return new WaterSourceResponse(source.getId(), source.getFarmId(), source.getName(), source.getType(),
                source.getCapacityLiters(), source.getCurrentLevelLiters(), source.getLatitude(),
                source.getLongitude(), source.getCreatedAt());
    }
}