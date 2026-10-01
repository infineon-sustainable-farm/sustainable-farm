package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Intervention de maintenance du goutte-a-goutte envoyee par un client (module 6.1).
 *
 * <p>Le client ne fournit ni identifiant ni date de creation : ils appartiennent au serveur.</p>
 */
public record DripMaintenanceLogRequest(
        UUID zoneId,
        Instant maintenanceDate,
        String maintenanceType,
        Boolean filterCleaned,
        Boolean cloggingDetected,
        String cloggingSeverity,
        Integer emitterReplacedCount,
        String notes,
        String performedBy) {
}
