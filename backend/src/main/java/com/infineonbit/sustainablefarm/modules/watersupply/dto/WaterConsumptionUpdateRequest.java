package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import java.time.Instant;

public record WaterConsumptionUpdateRequest(
        Long farmId,
        Long sourceId,
        Double consumptionLiters,
        Instant consumptionDate,
        Long irrigationId,
        Long zoneId,
        String lotId,
        String activity) {
}
