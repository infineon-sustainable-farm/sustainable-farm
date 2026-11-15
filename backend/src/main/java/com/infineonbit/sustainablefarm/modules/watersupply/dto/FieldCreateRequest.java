package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record FieldCreateRequest(
        @NotNull UUID farmId,
        @NotBlank String name,
        @NotNull Double areaHectares,
        String cropType,
        String soilType,
        String coordinates) {
}