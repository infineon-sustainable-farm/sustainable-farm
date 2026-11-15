package com.infineonbit.sustainablefarm.modules.plants.dto.Request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request to record plants of a nursery batch lost, for example dead plants or
 * failed grafts.
 *
 * <p>Validated like {@link NurseryBatchRequest}: a 400 whose
 * {@code fieldErrors} lists each failing component. The reason is trimmed by
 * the service. The quantity is read as a decimal number so that 12.5 is
 * refused rather than cut to 12; its constraints share one message.
 *
 * <p>The service refuses, with a 422, a loss dated before the start of the
 * batch or larger than the plants left in it.
 */
public record NurseryLossRequest(
        @NotNull(message = "lostOn is required")
        @PastOrPresent(message = "lostOn must be today or in the past")
        LocalDate lostOn,

        @Schema(description = "Whole number, 1 or more", example = "12")
        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be a whole number, 1 or more")
        @Digits(integer = 9, fraction = 0, message = "quantity must be a whole number, 1 or more")
        Double quantity,

        @Schema(example = "Graft failure")
        @NotBlank(message = "reason is required")
        @Size(max = 255, message = "reason must be at most 255 characters")
        String reason) {
}
