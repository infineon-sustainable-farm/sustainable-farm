package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import java.time.Instant;
import java.util.UUID;

public record IrrigationScheduleUpdateRequest(
        UUID zoneId,
        Instant startTime,
        Integer durationMinutes,
        Double waterQuantityLiters,
        String status,
        UUID createdBy) {
}