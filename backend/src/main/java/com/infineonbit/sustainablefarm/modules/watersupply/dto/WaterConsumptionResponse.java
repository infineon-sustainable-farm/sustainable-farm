package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterConsumption;
import java.time.Instant;
import java.util.UUID;

public record WaterConsumptionResponse(UUID id, UUID farmId, UUID sourceId, Double consumptionLiters,
                                       Instant consumptionDate, UUID irrigationId, UUID zoneId,
                                       String lotId, String activity, String blockCode, Boolean multipleBlockCodes,
                                       Instant createdAt) {
    public static WaterConsumptionResponse from(WaterConsumption consumption) {
        return from(consumption, null, false);
    }

    public static WaterConsumptionResponse from(WaterConsumption consumption, String blockCode, boolean multipleBlockCodes) {
        return new WaterConsumptionResponse(consumption.getId(), consumption.getFarmId(), consumption.getSourceId(),
                consumption.getConsumptionLiters(), consumption.getConsumptionDate(), consumption.getIrrigationId(),
                consumption.getZoneId(), consumption.getLotId(), consumption.getActivity(), blockCode, multipleBlockCodes,
                consumption.getCreatedAt());
    }
}
