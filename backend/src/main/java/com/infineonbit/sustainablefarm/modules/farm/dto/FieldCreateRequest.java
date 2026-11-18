package com.infineonbit.sustainablefarm.modules.farm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FieldCreateRequest(
        @NotNull Long farmId,
        @NotBlank String name,
        @NotNull Double areaHectares,
        String cropType,
        String soilType,
        String coordinates) {
}