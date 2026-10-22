package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ZoneBlockMappingCreateRequest(
        @NotNull UUID zoneId,
        @NotBlank String blockCode,
        @NotNull UUID farmId) {
}
