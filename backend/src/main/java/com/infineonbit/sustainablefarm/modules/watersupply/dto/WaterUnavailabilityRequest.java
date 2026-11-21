package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record WaterUnavailabilityRequest(
        @NotNull String scope,
        @NotNull Long scopeId,
        @NotNull Instant startAt,
        @NotNull Instant endAt,
        String reason,
        String status,
        Boolean postponeIrrigations) {
}
