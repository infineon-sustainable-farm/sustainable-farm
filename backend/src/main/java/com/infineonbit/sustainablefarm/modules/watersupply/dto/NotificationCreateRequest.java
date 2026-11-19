package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotificationCreateRequest(
        @NotNull Long userId,
        @NotBlank String title,
        @NotBlank String message,
        @NotBlank String type,
        Boolean read,
        String actionUrl) {
}