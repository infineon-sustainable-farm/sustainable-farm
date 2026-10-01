package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record WaterQualityCreateRequest(
        @NotNull UUID sourceId,
        Double ph,
        Double temperatureCelsius,
        Double turbidityNtu,
        Double conductivityUsCm,
        Double salinityPpt,
        Instant testDate) {
}