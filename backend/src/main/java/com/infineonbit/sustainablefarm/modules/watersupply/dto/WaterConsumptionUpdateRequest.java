package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import java.time.Instant;
import java.util.UUID;

public record WaterConsumptionUpdateRequest(
        Long farmId,
        Long sourceId,
        Double consumptionLiters,
        Instant consumptionDate,
        UUID irrigationId,
        Long zoneId,
        String lotId,
        String activity) {
}
