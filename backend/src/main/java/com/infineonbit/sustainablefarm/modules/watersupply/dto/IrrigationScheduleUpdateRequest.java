package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import java.time.Instant;

public record IrrigationScheduleUpdateRequest(
        Long zoneId,
        Instant startTime,
        Integer durationMinutes,
        Double waterQuantityLiters,
        String status,
        Long createdBy) {
}