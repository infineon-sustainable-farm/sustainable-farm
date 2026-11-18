package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import java.time.Instant;

public record WaterQualityUpdateRequest(
        Long sourceId,
        Double ph,
        Double temperatureCelsius,
        Double turbidityNtu,
        Double conductivityUsCm,
        Double salinityPpt,
        Instant testDate) {
}