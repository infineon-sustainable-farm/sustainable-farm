package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import java.time.Instant;
import java.util.UUID;

public record WaterConsumptionUpdateRequest(
        UUID farmId,
        UUID sourceId,
        Double consumptionLiters,
        Instant consumptionDate,
        UUID irrigationId) {
}