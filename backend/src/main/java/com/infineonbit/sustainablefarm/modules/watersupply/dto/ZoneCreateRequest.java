package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ZoneCreateRequest(
        @NotNull UUID fieldId,
        @NotBlank String name,
        @NotNull Double areaHectares,
        String irrigationMethod) {
}