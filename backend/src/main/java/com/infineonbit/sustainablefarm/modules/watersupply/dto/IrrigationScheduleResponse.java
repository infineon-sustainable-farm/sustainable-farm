package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationSchedule;
import java.time.Instant;
import java.util.UUID;

public record IrrigationScheduleResponse(UUID id, UUID zoneId, Instant startTime, Integer durationMinutes,
                                         Double waterQuantityLiters, String status, UUID createdBy, Instant createdAt,
                                         String postponeReason) {
    public static IrrigationScheduleResponse from(IrrigationSchedule schedule) {
        return new IrrigationScheduleResponse(
            schedule.getId(), schedule.getZoneId(), schedule.getStartTime(),
            schedule.getDurationMinutes(), schedule.getWaterQuantityLiters(), schedule.getStatus(),
            schedule.getCreatedBy(), schedule.getCreatedAt(), schedule.getPostponeReason());
    }
}
