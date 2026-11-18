package com.infineonbit.sustainablefarm.modules.notification.api;

import java.time.Instant;

/** An alert as the modules read it. The attributes are those of {@link AlertDetails}. */
public record AlertDto(
        Long id,
        SourceModule source,
        String code,
        String type,
        AlertSeverity severity,
        AlertStatus status,
        String title,
        String message,
        String relatedEntityType,
        String relatedEntityId,
        String region,
        String origin,
        Long recipientPersonId,
        boolean read,
        String actionUrl,
        Instant raisedAt,
        Instant resolvedAt,
        Instant createdAt,
        Instant updatedAt) {
}
