package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record NotificationCreateRequest(
        @NotNull UUID userId,
        @NotBlank String title,
        @NotBlank String message,
        @NotBlank String type,
        Boolean read,
        String actionUrl) {
}