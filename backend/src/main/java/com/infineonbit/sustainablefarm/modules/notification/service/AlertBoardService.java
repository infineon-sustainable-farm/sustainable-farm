package com.infineonbit.sustainablefarm.modules.notification.service;

import com.infineonbit.sustainablefarm.modules.notification.api.AlertBoard;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertDetails;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertDto;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertStatus;
import com.infineonbit.sustainablefarm.modules.notification.api.SourceModule;
import com.infineonbit.sustainablefarm.modules.notification.entity.Alert;
import com.infineonbit.sustainablefarm.modules.notification.repository.AlertRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AlertBoardService implements AlertBoard {

    private final AlertRepository repository;

    public AlertBoardService(AlertRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public AlertDto raise(SourceModule source, AlertDetails details) {
        Objects.requireNonNull(source, "source");
        Alert alert = new Alert();
        alert.setSource(source);
        apply(alert, details);
        if (alert.getRaisedAt() == null) {
            alert.setRaisedAt(Instant.now());
        }
        return toDto(repository.saveAndFlush(alert));
    }

    @Override
    @Transactional
    public Optional<AlertDto> raiseOnce(SourceModule source, AlertDetails details, Duration window) {
        Instant threshold = Instant.now().minus(window);
        if (repository.existsBySourceAndTitleAndReadIsFalseAndRaisedAtAfter(source, details.title(), threshold)) {
            return Optional.empty();
        }
        return Optional.of(raise(source, details));
    }

    @Override
    public Optional<AlertDto> find(SourceModule source, Long alertId) {
        return entity(source, alertId).map(AlertBoardService::toDto);
    }

    @Override
    public List<AlertDto> list(SourceModule source, AlertStatus status) {
        List<Alert> alerts = status == null
                ? repository.findBySourceOrderByIdAsc(source)
                : repository.findBySourceAndStatusOrderByIdAsc(source, status);
        return alerts.stream().map(AlertBoardService::toDto).toList();
    }

    @Override
    public Page<AlertDto> page(SourceModule source, Long recipientPersonId, Pageable pageable) {
        Page<Alert> page = recipientPersonId == null
                ? repository.findBySource(source, pageable)
                : repository.findBySourceAndRecipientPersonId(source, recipientPersonId, pageable);
        return page.map(AlertBoardService::toDto);
    }

    @Override
    @Transactional
    public Optional<AlertDto> update(SourceModule source, Long alertId, AlertDetails details) {
        return entity(source, alertId).map(alert -> {
            Instant raisedAt = alert.getRaisedAt();
            apply(alert, details);
            if (alert.getRaisedAt() == null) {
                alert.setRaisedAt(raisedAt);
            }
            return toDto(repository.saveAndFlush(alert));
        });
    }

    @Override
    @Transactional
    public Optional<AlertDto> changeStatus(SourceModule source, Long alertId, AlertStatus status) {
        Objects.requireNonNull(status, "status");
        return entity(source, alertId).map(alert -> {
            alert.setStatus(status);
            if (status == AlertStatus.RESOLVED && alert.getResolvedAt() == null) {
                alert.setResolvedAt(Instant.now());
            }
            return toDto(repository.saveAndFlush(alert));
        });
    }

    @Override
    @Transactional
    public Optional<AlertDto> markRead(SourceModule source, Long alertId) {
        return entity(source, alertId).map(alert -> {
            alert.setRead(true);
            return toDto(repository.saveAndFlush(alert));
        });
    }

    @Override
    @Transactional
    public List<AlertDto> markAllRead(SourceModule source) {
        List<Alert> alerts = repository.findBySourceOrderByIdAsc(source);
        alerts.forEach(alert -> alert.setRead(true));
        return repository.saveAllAndFlush(alerts).stream().map(AlertBoardService::toDto).toList();
    }

    @Override
    @Transactional
    public boolean delete(SourceModule source, Long alertId) {
        Optional<Alert> alert = entity(source, alertId);
        alert.ifPresent(repository::delete);
        return alert.isPresent();
    }

    @Override
    public long count(SourceModule source) {
        return repository.countBySource(source);
    }

    @Override
    public long countByType(SourceModule source, String type) {
        return repository.countBySourceAndTypeIgnoreCase(source, type);
    }

    @Override
    public boolean isAbout(SourceModule source, String relatedEntityType, String relatedEntityId) {
        return repository.existsBySourceAndRelatedEntityTypeAndRelatedEntityId(source, relatedEntityType, relatedEntityId);
    }

    private Optional<Alert> entity(SourceModule source, Long alertId) {
        return alertId == null ? Optional.empty() : repository.findByIdAndSource(alertId, source);
    }

    private static void apply(Alert alert, AlertDetails details) {
        alert.setCode(details.code());
        alert.setType(details.type());
        alert.setSeverity(details.severity());
        alert.setStatus(details.status() == null ? AlertStatus.OPEN : details.status());
        alert.setTitle(details.title());
        alert.setMessage(details.message());
        alert.setRelatedEntityType(details.relatedEntityType());
        alert.setRelatedEntityId(details.relatedEntityId());
        alert.setRegion(details.region());
        alert.setOrigin(details.origin());
        alert.setRecipientPersonId(details.recipientPersonId());
        alert.setRead(Boolean.TRUE.equals(details.read()));
        alert.setActionUrl(details.actionUrl());
        alert.setRaisedAt(details.raisedAt());
        alert.setResolvedAt(details.resolvedAt());
    }

    private static AlertDto toDto(Alert alert) {
        return new AlertDto(alert.getId(), alert.getSource(), alert.getCode(), alert.getType(), alert.getSeverity(),
                alert.getStatus(), alert.getTitle(), alert.getMessage(), alert.getRelatedEntityType(),
                alert.getRelatedEntityId(), alert.getRegion(), alert.getOrigin(), alert.getRecipientPersonId(),
                alert.isRead(), alert.getActionUrl(), alert.getRaisedAt(), alert.getResolvedAt(),
                alert.getCreatedAt(), alert.getUpdatedAt());
    }
}
