package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WaterSourceCreateRequest(
        @NotNull Long farmId,
        @NotBlank String name,
        @NotBlank String type,
        @NotNull Double capacityLiters,
        @NotNull Double currentLevelLiters,
        Double latitude,
        Double longitude) {
}