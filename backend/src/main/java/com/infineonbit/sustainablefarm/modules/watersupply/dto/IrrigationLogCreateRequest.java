package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record IrrigationLogCreateRequest(
        @NotNull Long scheduleId,
        @NotNull Instant actualStartTime,
        Instant actualEndTime,
        @NotNull Double waterUsedLiters,
        @NotNull String status) {
}