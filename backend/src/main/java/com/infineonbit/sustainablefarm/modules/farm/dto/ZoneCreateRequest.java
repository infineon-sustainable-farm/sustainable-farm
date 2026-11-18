package com.infineonbit.sustainablefarm.modules.farm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ZoneCreateRequest(
        @NotNull Long fieldId,
        @NotBlank String name,
        @NotNull Double areaHectares,
        String irrigationMethod,
        Double cropCoefficient,
        @PositiveOrZero Integer emitterCount,
        @PositiveOrZero Double emitterNominalFlowLh) {
}