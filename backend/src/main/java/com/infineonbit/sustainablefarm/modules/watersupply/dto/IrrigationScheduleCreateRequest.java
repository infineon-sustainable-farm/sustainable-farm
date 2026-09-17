package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record IrrigationScheduleCreateRequest(
        @NotNull UUID zoneId,
        @NotNull Instant startTime,
        @NotNull Integer durationMinutes,
        @NotNull Double waterQuantityLiters,
        String status,
        UUID createdBy) {
}