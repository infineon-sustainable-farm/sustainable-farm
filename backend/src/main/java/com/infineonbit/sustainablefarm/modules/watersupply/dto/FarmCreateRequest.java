package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import jakarta.validation.constraints.NotBlank;

public record FarmCreateRequest(
        @NotBlank String name,
        String description,
        String address,
        Double latitude,
        Double longitude,
        Double areaHectares) {
}