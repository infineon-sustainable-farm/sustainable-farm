package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.DripMaintenanceLog;
import java.time.Instant;

/** API view of a maintenance intervention: stable contract, independent of the JPA entity. */
public record DripMaintenanceLogResponse(
        Long id,
        Long zoneId,
        Instant maintenanceDate,
        String maintenanceType,
        Boolean filterCleaned,
        Boolean cloggingDetected,
        String cloggingSeverity,
        Integer emitterReplacedCount,
        String notes,
        String performedBy,
        Instant createdAt) {

    /**
     * @param performedBy full name of the person referenced by {@code performedByPersonId}
     */
    public static DripMaintenanceLogResponse from(DripMaintenanceLog log, String performedBy) {
        return new DripMaintenanceLogResponse(log.getId(), log.getZoneId(), log.getMaintenanceDate(),
                log.getMaintenanceType(), log.getFilterCleaned(), log.getCloggingDetected(),
                log.getCloggingSeverity(), log.getEmitterReplacedCount(), log.getNotes(),
                performedBy, log.getCreatedAt());
    }
}
