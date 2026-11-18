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
 * {@link #COMPONENT_ENTITY} with the component id.
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
                        component == null ? null : component.getComponentId())
                .severity(com.infineonbit.sustainablefarm.modules.notification.api.AlertSeverity.valueOf(severity.name()))
                .message(message)
                .raisedAt(timestamp)
                .status(com.infineonbit.sustainablefarm.modules.notification.api.AlertStatus.valueOf(status.name()))
                .build());
        return EnergyAlertResponse.from(alert, component == null ? null : component.getName());
    }

    public long count() {
        return alertBoard.count(SOURCE);
    }

    /** Whether an alert is about this component. */
    public boolean isAboutComponent(String componentId) {
        return alertBoard.isAbout(SOURCE, COMPONENT_ENTITY, componentId);
    }

    private List<EnergyAlertResponse> toResponses(List<AlertDto> alerts) {
        List<String> componentIds = alerts.stream()
                .filter(alert -> COMPONENT_ENTITY.equals(alert.relatedEntityType()))
                .map(AlertDto::relatedEntityId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, String> names = componentRepository.findAllById(componentIds).stream()
                .collect(Collectors.toMap(EnergyComponent::getComponentId,
                        component -> component.getName() == null ? "" : component.getName()));
        Function<AlertDto, String> nameOf = alert -> {
            String name = names.get(alert.relatedEntityId());
            return name == null || name.isEmpty() ? null : name;
        };
        return alerts.stream().map(alert -> EnergyAlertResponse.from(alert, nameOf.apply(alert))).toList();
    }

    private static EnergySupplyNotFoundException notFound(Long alertId) {
        return new EnergySupplyNotFoundException("Energy alert", String.valueOf(alertId));
    }
}
