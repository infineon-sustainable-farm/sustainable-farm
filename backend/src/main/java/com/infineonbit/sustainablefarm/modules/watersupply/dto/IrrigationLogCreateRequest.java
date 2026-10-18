package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record IrrigationLogCreateRequest(
        @NotNull UUID scheduleId,
        @NotNull Instant actualStartTime,
        Instant actualEndTime,
        @NotNull Double waterUsedLiters,
        @NotNull String status) {
}