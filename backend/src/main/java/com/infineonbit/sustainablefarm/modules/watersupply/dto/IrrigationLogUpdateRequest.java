package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import java.time.Instant;
import java.util.UUID;

public record IrrigationLogUpdateRequest(
        UUID scheduleId,
        Instant actualStartTime,
        Instant actualEndTime,
        Double waterUsedLiters,
        String status) {
}