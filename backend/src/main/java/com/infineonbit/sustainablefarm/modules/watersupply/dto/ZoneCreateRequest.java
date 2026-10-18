package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

public record ZoneCreateRequest(
        @NotNull UUID fieldId,
        @NotBlank String name,
        @NotNull Double areaHectares,
        String irrigationMethod,
        Double cropCoefficient,
        @PositiveOrZero Integer emitterCount,
        @PositiveOrZero Double emitterNominalFlowLh) {
}