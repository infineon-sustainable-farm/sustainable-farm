package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record IrrigationScheduleCreateRequest(
        @NotNull Long zoneId,
        @NotNull Instant startTime,
        @NotNull Integer durationMinutes,
        @NotNull Double waterQuantityLiters,
        String status,
        Long createdBy) {
}