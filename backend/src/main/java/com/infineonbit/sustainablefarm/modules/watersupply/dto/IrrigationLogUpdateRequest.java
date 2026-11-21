package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import java.time.Instant;

public record IrrigationLogUpdateRequest(
        Long scheduleId,
        Instant actualStartTime,
        Instant actualEndTime,
        Double waterUsedLiters,
        String status) {
}