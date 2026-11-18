package com.infineonbit.sustainablefarm.modules.farm.dto;

public record FarmUpdateRequest(
        String name,
        String description,
        String address,
        Double latitude,
        Double longitude,
        Double areaHectares) {
}