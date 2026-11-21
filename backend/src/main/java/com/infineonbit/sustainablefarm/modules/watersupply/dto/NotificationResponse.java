package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import com.infineonbit.sustainablefarm.modules.notification.api.AlertDto;
import java.time.Instant;

/** A water supply notification: an alert of the notification module, under its former JSON. */
public record NotificationResponse(Long id, Long userId, String title, String message,
                                   String type, Boolean read, String actionUrl, Instant createdAt) {
    public static NotificationResponse from(AlertDto alert) {
        return new NotificationResponse(alert.id(), alert.recipientPersonId(), alert.title(), alert.message(),
                alert.type(), alert.read(), alert.actionUrl(), alert.raisedAt());
    }
}
