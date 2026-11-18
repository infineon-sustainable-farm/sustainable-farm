package com.infineonbit.sustainablefarm.modules.notification.api;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * The single alert store of the application (ADR 0004, table {@value #TABLE}).
 *
 * <p>Energy supply, sales and marketing and water supply raise, read and change their alerts
 * here; each one keeps its own endpoints and JSON. Every method is scoped to the
 * {@link SourceModule} of the caller: a module never sees nor changes another module's alerts,
 * and an id of another module is treated as unknown.</p>
 *
 * <p>Lists are ordered by id, that is by creation order, unless a {@link Pageable} sorts them.
 * The attributes of a {@link Pageable} sort are those of {@link AlertDto}.</p>
 */
public interface AlertBoard {

    /** Table of the alerts. */
    String TABLE = "alert";

    /** Raises an alert. */
    AlertDto raise(SourceModule source, AlertDetails details);

    /**
     * Raises an alert unless the same one is already pending: an unread alert of the same
     * source with the same title raised within {@code window}.
     *
     * @return the alert raised, or empty when it was a duplicate
     */
    Optional<AlertDto> raiseOnce(SourceModule source, AlertDetails details, Duration window);

    Optional<AlertDto> find(SourceModule source, Long alertId);

    /** The alerts of a source, all of them for a {@code null} status. */
    List<AlertDto> list(SourceModule source, AlertStatus status);

    /** A page of the alerts of a source, all recipients for a {@code null} recipient. */
    Page<AlertDto> page(SourceModule source, Long recipientPersonId, Pageable pageable);

    /**
     * Replaces every writable attribute of an alert, as given: a {@code null} clears it, except
     * a {@code null} {@code raisedAt}, which keeps the time the alert was raised.
     */
    Optional<AlertDto> update(SourceModule source, Long alertId, AlertDetails details);

    /** Changes the status of an alert; {@link AlertStatus#RESOLVED} also stamps the resolution time. */
    Optional<AlertDto> changeStatus(SourceModule source, Long alertId, AlertStatus status);

    Optional<AlertDto> markRead(SourceModule source, Long alertId);

    /** Marks every alert of a source as read and returns them all. */
    List<AlertDto> markAllRead(SourceModule source);

    /** @return whether the alert existed */
    boolean delete(SourceModule source, Long alertId);

    long count(SourceModule source);

    /** Number of alerts of a source whose type is {@code type}, ignoring case. */
    long countByType(SourceModule source, String type);

    /** Whether an alert of a source is about this entity. */
    boolean isAbout(SourceModule source, String relatedEntityType, String relatedEntityId);
}
