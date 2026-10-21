
package com.infineonbit.sustainablefarm.modules.salesmarketing.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record AlertRequest(
        String alertCode,
        String alertType,
        @NotBlank(message = "severity is required") String severity,
        @NotBlank(message = "message is required") String message,
        String region,
        String origin,
        String relatedTable,
        Integer relatedId,
        String status,
        LocalDateTime resolvedAt
) {}
