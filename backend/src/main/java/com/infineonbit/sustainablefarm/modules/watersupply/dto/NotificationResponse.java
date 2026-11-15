package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.Notification;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(UUID id, UUID userId, String title, String message,
                                   String type, Boolean read, String actionUrl, Instant createdAt) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getUserId(), notification.getTitle(),
                notification.getMessage(), notification.getType(), notification.getRead(), notification.getActionUrl(),
                notification.getCreatedAt());
    }
}