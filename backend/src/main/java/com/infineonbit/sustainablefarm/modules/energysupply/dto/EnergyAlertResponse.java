package com.infineonbit.sustainablefarm.modules.energysupply.dto;

import com.infineonbit.sustainablefarm.modules.energysupply.entity.EnergyComponent;
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
     * @param alert     an alert of the energy supply module
     * @param component the component the alert is about, {@code null} when none
     */
    public static EnergyAlertResponse from(AlertDto alert, EnergyComponent component) {
        String componentName = component == null || component.getName() == null || component.getName().isEmpty()
                ? null : component.getName();
        return new EnergyAlertResponse(
                alert.id(),
                component == null ? null : component.getCode(),
                componentName,
                alert.severity() == null ? null : AlertSeverity.valueOf(alert.severity().name()),
                alert.message(),
                alert.raisedAt() == null ? null : LocalDateTime.ofInstant(alert.raisedAt(), ZoneId.systemDefault()),
                AlertStatus.valueOf(alert.status().name()));
    }
}
