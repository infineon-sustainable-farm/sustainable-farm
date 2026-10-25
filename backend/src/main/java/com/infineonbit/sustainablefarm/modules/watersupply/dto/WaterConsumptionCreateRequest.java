package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record WaterConsumptionCreateRequest(
        @NotNull UUID farmId,
        @NotNull UUID sourceId,
        @NotNull Double consumptionLiters,
        @NotNull Instant consumptionDate,
        UUID irrigationId,
        UUID zoneId,
        String lotId,
        String activity) {
    public WaterConsumptionCreateRequest(UUID farmId, UUID sourceId, Double consumptionLiters,
                                         Instant consumptionDate, UUID irrigationId, UUID zoneId) {
        this(farmId, sourceId, consumptionLiters, consumptionDate, irrigationId, zoneId, null, null);
    }
}
