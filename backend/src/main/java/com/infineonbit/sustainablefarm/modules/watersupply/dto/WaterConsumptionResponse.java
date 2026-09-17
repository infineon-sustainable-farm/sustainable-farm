package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterConsumption;
import java.time.Instant;
import java.util.UUID;

public record WaterConsumptionResponse(UUID id, UUID farmId, UUID sourceId, Double consumptionLiters,
                                       Instant consumptionDate, UUID irrigationId, Instant createdAt) {
    public static WaterConsumptionResponse from(WaterConsumption consumption) {
        return new WaterConsumptionResponse(consumption.getId(), consumption.getFarmId(), consumption.getSourceId(),
                consumption.getConsumptionLiters(), consumption.getConsumptionDate(), consumption.getIrrigationId(),
                consumption.getCreatedAt());
    }
}