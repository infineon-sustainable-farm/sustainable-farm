package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record WaterSourceCreateRequest(
        @NotNull UUID farmId,
        @NotBlank String name,
        @NotBlank String type,
        @NotNull Double capacityLiters,
        @NotNull Double currentLevelLiters,
        Double latitude,
        Double longitude) {
}