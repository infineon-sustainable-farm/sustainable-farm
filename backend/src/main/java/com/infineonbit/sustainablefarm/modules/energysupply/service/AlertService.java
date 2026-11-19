package com.infineonbit.sustainablefarm.modules.energysupply.service;

import com.infineonbit.sustainablefarm.modules.energysupply.dto.AlertSeverity;
import com.infineonbit.sustainablefarm.modules.energysupply.dto.AlertStatus;
import com.infineonbit.sustainablefarm.modules.energysupply.dto.EnergyAlertResponse;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.EnergyComponent;
import com.infineonbit.sustainablefarm.modules.energysupply.exception.EnergySupplyNotFoundException;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.EnergyComponentRepository;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertBoard;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertDetails;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertDto;
import com.infineonbit.sustainablefarm.modules.notification.api.SourceModule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The alerts of the Energy App, kept in the single alert store of the notification module
 * (ADR 0004). An alert about a component references it as the related entity
 * {@link #COMPONENT_ENTITY} with the component's Long id; the API shows the component's
 * business code as {@code componentId}.
 */
@Service("energyAlertService")
@Transactional(readOnly = true)
public class AlertService {

    /** Related entity type of an alert about an energy component. */
    public static final String COMPONENT_ENTITY = "energy_component";

    private static final SourceModule SOURCE = SourceModule.ENERGYSUPPLY;

    private final AlertBoard alertBoard;
    private final EnergyComponentRepository componentRepository;

    public AlertService(AlertBoard alertBoard, EnergyComponentRepository componentRepository) {
        this.alertBoard = alertBoard;
        this.componentRepository = componentRepository;
    }

    public List<EnergyAlertResponse> findAll() {
        return toResponses(alertBoard.list(SOURCE, null));
    }

    public List<EnergyAlertResponse> findOpen() {
        return toResponses(alertBoard.list(SOURCE, com.infineonbit.sustainablefarm.modules.notification.api.AlertStatus.OPEN));
    }

    @Transactional
    public EnergyAlertResponse updateStatus(Long alertId, AlertStatus status) {
        AlertDto alert = alertBoard.find(SOURCE, alertId)
                .orElseThrow(() -> notFound(alertId));
        if (alert.status() == com.infineonbit.sustainablefarm.modules.notification.api.AlertStatus.RESOLVED
                && status == AlertStatus.OPEN) {
            throw new IllegalArgumentException("Resolved alerts cannot be reopened");
        }
        AlertDto changed = alertBoard.changeStatus(SOURCE, alertId,
                        com.infineonbit.sustainablefarm.modules.notification.api.AlertStatus.valueOf(status.name()))
                .orElseThrow(() -> notFound(alertId));
        return toResponses(List.of(changed)).get(0);
    }

    /** Raises an alert about a component ({@code null} for none). */
    @Transactional
    public EnergyAlertResponse raise(EnergyComponent component, AlertSeverity severity, String message,
                                     Instant timestamp, AlertStatus status) {
        AlertDto alert = alertBoard.raise(SOURCE, AlertDetails.builder()
                .relatedEntity(component == null ? null : COMPONENT_ENTITY,
                        component == null ? null : relatedId(component))
                .severity(com.infineonbit.sustainablefarm.modules.notification.api.AlertSeverity.valueOf(severity.name()))
                .message(message)
                .raisedAt(timestamp)
                .status(com.infineonbit.sustainablefarm.modules.notification.api.AlertStatus.valueOf(status.name()))
                .build());
        return EnergyAlertResponse.from(alert, component);
    }

    public long count() {
        return alertBoard.count(SOURCE);
    }

    /** Whether an alert is about this component. */
    public boolean isAboutComponent(EnergyComponent component) {
        return alertBoard.isAbout(SOURCE, COMPONENT_ENTITY, relatedId(component));
    }

    private List<EnergyAlertResponse> toResponses(List<AlertDto> alerts) {
        List<Long> componentIds = alerts.stream()
                .map(AlertService::componentIdOf)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, EnergyComponent> components = componentRepository.findAllById(componentIds).stream()
                .collect(Collectors.toMap(EnergyComponent::getId, Function.identity()));
        return alerts.stream()
                .map(alert -> EnergyAlertResponse.from(alert, components.get(componentIdOf(alert))))
                .toList();
    }

    private static String relatedId(EnergyComponent component) {
        return String.valueOf(component.getId());
    }

    /** The Long id of the component an alert is about, {@code null} when none. */
    private static Long componentIdOf(AlertDto alert) {
        if (!COMPONENT_ENTITY.equals(alert.relatedEntityType()) || alert.relatedEntityId() == null) {
            return null;
        }
        try {
            return Long.valueOf(alert.relatedEntityId());
        } catch (NumberFormatException notAnId) {
            return null;
        }
    }

    private static EnergySupplyNotFoundException notFound(Long alertId) {
        return new EnergySupplyNotFoundException("Energy alert", String.valueOf(alertId));
    }
}
