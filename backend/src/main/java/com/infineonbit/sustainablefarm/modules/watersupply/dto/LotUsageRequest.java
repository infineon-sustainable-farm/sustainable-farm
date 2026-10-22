package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/**
 * Attributes the consumptions measured in [startAt, endAt[ (optionally on one source and/or zone)
 * to a Product Transformation lot and activity.
 */
public record LotUsageRequest(
        @NotBlank @Size(max = 128) String lotId,
        String activity,
        @NotNull Instant startAt,
        @NotNull Instant endAt,
        UUID sourceId,
        UUID zoneId) {
}
