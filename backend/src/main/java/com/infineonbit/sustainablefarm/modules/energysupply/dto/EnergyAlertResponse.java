package com.infineonbit.sustainablefarm.modules.energysupply.dto;

import com.infineonbit.sustainablefarm.modules.notification.api.AlertDto;

import java.time.LocalDateTime;
import java.time.ZoneId;

public record EnergyAlertResponse(
        Long alertId,
        String componentId,
        String componentName,
        AlertSeverity severity,
        String message,
        LocalDateTime timestamp,
        AlertStatus status) {

    /**
     * @param alert         an alert of the energy supply module
     * @param componentName name of the component the alert is about, {@code null} when none
     */
    public static EnergyAlertResponse from(AlertDto alert, String componentName) {
        return new EnergyAlertResponse(
                alert.id(),
                alert.relatedEntityId(),
                componentName,
                alert.severity() == null ? null : AlertSeverity.valueOf(alert.severity().name()),
                alert.message(),
                alert.raisedAt() == null ? null : LocalDateTime.ofInstant(alert.raisedAt(), ZoneId.systemDefault()),
                AlertStatus.valueOf(alert.status().name()));
    }
}
