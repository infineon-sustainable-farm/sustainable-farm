package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.Farm;
import java.time.Instant;
import java.util.UUID;

public record FarmResponse(UUID id, String name, String description, String address,
                           Double latitude, Double longitude, Double areaHectares, Instant createdAt) {
    public static FarmResponse from(Farm farm) {
        return new FarmResponse(farm.getId(), farm.getName(), farm.getDescription(), farm.getAddress(),
                farm.getLatitude(), farm.getLongitude(), farm.getAreaHectares(), farm.getCreatedAt());
    }
}