package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record WaterQualityCreateRequest(
        @NotNull Long sourceId,
        Double ph,
        Double temperatureCelsius,
        Double turbidityNtu,
        Double conductivityUsCm,
        Double salinityPpt,
        Instant testDate) {
}