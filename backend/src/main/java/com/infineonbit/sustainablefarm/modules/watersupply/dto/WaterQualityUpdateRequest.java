package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import java.time.Instant;
import java.util.UUID;

public record WaterQualityUpdateRequest(
        UUID sourceId,
        Double ph,
        Double temperatureCelsius,
        Double turbidityNtu,
        Double conductivityUsCm,
        Double salinityPpt,
        Instant testDate) {
}