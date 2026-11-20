package com.infineonbit.sustainablefarm.modules.farm.dto;

import com.infineonbit.sustainablefarm.modules.farm.entity.Farm;
import java.time.Instant;

public record FarmResponse(Long id, String name, String description, String address,
                           Double latitude, Double longitude, Double areaHectares, Instant createdAt) {
    public static FarmResponse from(Farm farm) {
        return new FarmResponse(farm.getId(), farm.getName(), farm.getDescription(), farm.getAddress(),
                farm.getLatitude(), farm.getLongitude(), farm.getAreaHectares(), farm.getCreatedAt());
    }
}
