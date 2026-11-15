package com.infineonbit.sustainablefarm.modules.energysupply.dto;

import com.infineonbit.sustainablefarm.modules.energysupply.entity.Alert;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.AlertSeverity;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.AlertStatus;

import java.time.LocalDateTime;

public record EnergyAlertResponse(
        String alertId,
        String componentId,
        String componentName,
        AlertSeverity severity,
        String message,
        LocalDateTime timestamp,
        AlertStatus status) {

    public static EnergyAlertResponse from(Alert alert) {
        return new EnergyAlertResponse(
                alert.getAlertId(),
                alert.getComponent() == null ? null : alert.getComponent().getComponentId(),
                alert.getComponent() == null ? null : alert.getComponent().getName(),
                alert.getSeverity(),
                alert.getMessage(),
                alert.getTimestamp(),
                alert.getStatus());
    }
}
