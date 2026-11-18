package com.infineonbit.sustainablefarm.modules.farm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ZoneBlockMappingCreateRequest(
        @NotNull Long zoneId,
        @NotBlank String blockCode,
        @NotNull Long farmId) {
}
