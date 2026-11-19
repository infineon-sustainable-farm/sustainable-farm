package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertBoard;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertDetails;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertDto;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertSeverity;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertStatus;
import com.infineonbit.sustainablefarm.modules.notification.api.SourceModule;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.AlertRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.AlertResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

/**
 * The demand alerts of Sales and Marketing, kept in the single alert store of the notification
 * module (ADR 0004). Severity and status keep their lower-case values in the JSON.
 */
@Service("salesMarketingAlertService")
public class AlertService {

    private static final SourceModule SOURCE = SourceModule.SALESMARKETING;

    /** Message of a value refused by the former check constraints of the {@code alerts} table. */
    private static final String REFUSED_VALUE =
            "The request conflicts with the current state of the data. Please refresh and try again.";

    private final AlertBoard alertBoard;

    public AlertService(AlertBoard alertBoard) {
        this.alertBoard = alertBoard;
    }

    public List<AlertResponse> getAll() {
        return alertBoard.list(SOURCE, null).stream().map(AlertService::toResponse).toList();
    }

    public AlertResponse getById(Long id) {
        return toResponse(alertBoard.find(SOURCE, id).orElseThrow(() -> notFound(id)));
    }

    public AlertResponse create(AlertRequest request) {
        return toResponse(alertBoard.raise(SOURCE, details(request)));
    }

    public AlertResponse update(Long id, AlertRequest request) {
        return toResponse(alertBoard.update(SOURCE, id, details(request)).orElseThrow(() -> notFound(id)));
    }

    public void delete(Long id) {
        alertBoard.delete(SOURCE, id);
    }

    private static AlertDetails details(AlertRequest request) {
        return AlertDetails.builder()
                .code(request.alertCode())
                .type(request.alertType())
                .severity(severity(request.severity()))
                .message(request.message())
                .region(request.region())
                .origin(request.origin())
                .relatedEntity(request.relatedTable(),
                        request.relatedId() == null ? null : String.valueOf(request.relatedId()))
                .status(status(request.status()))
                .resolvedAt(toInstant(request.resolvedAt()))
                .build();
    }

    private static AlertResponse toResponse(AlertDto alert) {
        return new AlertResponse(
                alert.id(),
                alert.code(),
                alert.type(),
                alert.severity() == null ? null : alert.severity().name().toLowerCase(Locale.ROOT),
                alert.message(),
                alert.region(),
                alert.origin(),
                alert.relatedEntityType(),
                alert.relatedEntityId() == null ? null : Integer.valueOf(alert.relatedEntityId()),
                alert.status().name().toLowerCase(Locale.ROOT),
                toLocal(alert.raisedAt()),
                toLocal(alert.resolvedAt()));
    }

    /** The values of the former check constraint: critical, warning, info. */
    private static AlertSeverity severity(String value) {
        for (AlertSeverity severity : AlertSeverity.values()) {
            if (severity.name().toLowerCase(Locale.ROOT).equals(value)) {
                return severity;
            }
        }
        throw new ConflictException(REFUSED_VALUE);
    }

    /** The values of the former check constraint: open, acknowledged, resolved, dismissed. */
    private static AlertStatus status(String value) {
        if (value == null) {
            return AlertStatus.OPEN;
        }
        for (AlertStatus status : AlertStatus.values()) {
            if (status.name().toLowerCase(Locale.ROOT).equals(value)) {
                return status;
            }
        }
        throw new ConflictException(REFUSED_VALUE);
    }

    private static Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }

    private static LocalDateTime toLocal(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneId.systemDefault());
    }

    private static ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("Alert not found with id: " + id);
    }
}
