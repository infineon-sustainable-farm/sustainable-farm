package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationLog;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.StoredValue;
import java.time.Instant;

public record IrrigationLogResponse(Long id, Long scheduleId, Instant actualStartTime, Instant actualEndTime,
                                    Double waterUsedLiters, String status, Instant createdAt) {
    public static IrrigationLogResponse from(IrrigationLog log) {
        return new IrrigationLogResponse(log.getId(), log.getScheduleId(), log.getActualStartTime(),
                log.getActualEndTime(), log.getWaterUsedLiters(), StoredValue.valueOf(log.getStatus()), log.getCreatedAt());
    }
}