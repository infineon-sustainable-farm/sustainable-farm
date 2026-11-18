package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record WaterConsumptionCreateRequest(
        @NotNull Long farmId,
        @NotNull Long sourceId,
        @NotNull Double consumptionLiters,
        @NotNull Instant consumptionDate,
        UUID irrigationId,
        Long zoneId,
        String lotId,
        String activity) {
    public WaterConsumptionCreateRequest(Long farmId, Long sourceId, Double consumptionLiters,
                                         Instant consumptionDate, UUID irrigationId, Long zoneId) {
        this(farmId, sourceId, consumptionLiters, consumptionDate, irrigationId, zoneId, null, null);
    }
}
