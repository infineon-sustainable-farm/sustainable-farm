package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.DripMaintenanceLog;
import java.time.Instant;
import java.util.UUID;

/** API view of a maintenance intervention: stable contract, independent of the JPA entity. */
public record DripMaintenanceLogResponse(
        UUID id,
        UUID zoneId,
        Instant maintenanceDate,
        String maintenanceType,
        Boolean filterCleaned,
        Boolean cloggingDetected,
        String cloggingSeverity,
        Integer emitterReplacedCount,
        String notes,
        String performedBy,
        Instant createdAt) {

    public static DripMaintenanceLogResponse from(DripMaintenanceLog log) {
        return new DripMaintenanceLogResponse(log.getId(), log.getZoneId(), log.getMaintenanceDate(),
                log.getMaintenanceType(), log.getFilterCleaned(), log.getCloggingDetected(),
                log.getCloggingSeverity(), log.getEmitterReplacedCount(), log.getNotes(),
                log.getPerformedBy(), log.getCreatedAt());
    }
}
