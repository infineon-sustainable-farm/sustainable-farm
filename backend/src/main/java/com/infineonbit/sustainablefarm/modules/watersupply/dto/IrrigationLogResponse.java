package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationLog;
import java.time.Instant;
import java.util.UUID;

public record IrrigationLogResponse(UUID id, UUID scheduleId, Instant actualStartTime, Instant actualEndTime,
                                    Double waterUsedLiters, String status, Instant createdAt) {
    public static IrrigationLogResponse from(IrrigationLog log) {
        return new IrrigationLogResponse(log.getId(), log.getScheduleId(), log.getActualStartTime(),
                log.getActualEndTime(), log.getWaterUsedLiters(), log.getStatus(), log.getCreatedAt());
    }
}