package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record WaterUnavailabilityRequest(
        @NotNull String scope,
        @NotNull UUID scopeId,
        @NotNull Instant startAt,
        @NotNull Instant endAt,
        String reason,
        String status,
        Boolean postponeIrrigations) {
}
