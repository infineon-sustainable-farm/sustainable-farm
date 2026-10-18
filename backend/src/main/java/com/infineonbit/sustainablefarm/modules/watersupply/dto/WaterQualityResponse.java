package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterQualityTest;
import java.time.Instant;
import java.util.UUID;

public record WaterQualityResponse(UUID id, UUID sourceId, Double ph, Double temperatureCelsius,
                                   Double turbidityNtu, Double conductivityUsCm, Double salinityPpt,
                                   Instant testDate, Instant createdAt) {
    public static WaterQualityResponse from(WaterQualityTest test) {
        return new WaterQualityResponse(test.getId(), test.getSourceId(), test.getPh(), test.getTemperatureCelsius(),
                test.getTurbidityNtu(), test.getConductivityUsCm(), test.getSalinityPpt(), test.getTestDate(),
                test.getCreatedAt());
    }
}