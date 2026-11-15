package com.infineonbit.sustainablefarm.modules.plants.dto.Request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request to mark a planned application as done: what was really applied.
 *
 * <p>The fertilizer, the farm and the block are those of the plan. The date,
 * the quantity, the applicator and the method are those of an
 * {@link ApplicationRequest}, with the same rules and the same messages, since
 * the real application is recorded from them. The quantity may differ from the
 * planned one: the comparison of planned and applied quantities shows the gap.
 */
public record FertilizerPlanCompletionRequest(
        @NotNull(message = "applicationDate is required")
        @PastOrPresent(message = "applicationDate must be today or in the past")
        LocalDate applicationDate,

        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be greater than 0, with at most 3 decimals")
        @Digits(integer = 9, fraction = 3, message = "quantity must be greater than 0, with at most 3 decimals")
        Double quantity,

        @NotBlank(message = "applicator is required")
        @Size(max = 255, message = "applicator must be at most 255 characters")
        String applicator,

        @Size(max = 255, message = "method must be at most 255 characters")
        String method) {
}
