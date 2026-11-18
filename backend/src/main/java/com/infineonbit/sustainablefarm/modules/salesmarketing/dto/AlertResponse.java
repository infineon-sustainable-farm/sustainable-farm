package com.infineonbit.sustainablefarm.modules.salesmarketing.dto;

import java.time.LocalDateTime;

/** A sales alert, with the JSON of the former {@code alerts} entity. */
public record AlertResponse(
        Long alertId,
        String alertCode,
        String alertType,
        String severity,
        String message,
        String region,
        String origin,
        String relatedTable,
        Integer relatedId,
        String status,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt) {
}
