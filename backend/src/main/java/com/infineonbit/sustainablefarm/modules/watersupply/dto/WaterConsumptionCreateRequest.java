package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record WaterConsumptionCreateRequest(
        @NotNull UUID farmId,
        @NotNull UUID sourceId,
        @NotNull Double consumptionLiters,
        @NotNull Instant consumptionDate,
        UUID irrigationId) {
}