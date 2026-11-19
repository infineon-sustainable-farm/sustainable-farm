package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import java.time.Instant;

/**
 * Drip maintenance intervention sent by a client (module 6.1).
 *
 * <p>The client provides neither an id nor a creation date: they belong to the server.</p>
 */
public record DripMaintenanceLogRequest(
        Long zoneId,
        Instant maintenanceDate,
        String maintenanceType,
        Boolean filterCleaned,
        Boolean cloggingDetected,
        String cloggingSeverity,
        Integer emitterReplacedCount,
        String notes,
        /** Person of the people directory (GET /api/staff/persons) who performed it, or {@code null}. */
        Long performedByPersonId) {
}
