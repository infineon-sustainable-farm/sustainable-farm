package com.infineonbit.sustainablefarm.modules.plants.dto.Request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request to record a loss of a fertilizer, for example an expired or spilled
 * quantity, in the unit of that fertilizer.
 *
 * <p>Validated like {@link PurchaseRequest}: a 400 whose {@code fieldErrors}
 * lists each failing component. The reason is trimmed by the service. The
 * quantity has two constraints that share one message.
 */
public record LossRequest(
        @NotNull(message = "lossDate is required")
        @PastOrPresent(message = "lossDate must be today or in the past")
        LocalDate lossDate,

        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be greater than 0, with at most 3 decimals")
        @Digits(integer = 9, fraction = 3, message = "quantity must be greater than 0, with at most 3 decimals")
        Double quantity,

        @NotBlank(message = "reason is required")
        @Size(max = 255, message = "reason must be at most 255 characters")
        String reason) {
}
