package com.infineonbit.sustainablefarm.modules.plants.dto.Request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request to plan an application of a fertilizer on a block, in the unit of
 * that fertilizer.
 *
 * <p>The quantity is the farmer's own figure: the application recommends no
 * rate. Planning takes nothing from the stock, so a quantity larger than the
 * stock is accepted; the stock check of the plans warns about it.
 *
 * <p>Validated like {@link ApplicationRequest}: a 400 whose {@code fieldErrors}
 * lists each failing component. The block code has the same {@code @NotNull}
 * and pattern, and is normalized the same way by the service ({@code " b "}
 * becomes {@code "B"}); the note is trimmed. The quantity has two constraints
 * that share one message. The planned date is today or later: what is already
 * done is recorded as an application.
 */
public record FertilizerPlanRequest(
        @NotNull(message = "fertilizerId is required")
        Long fertilizerId,

        @Min(value = 1, message = "farmId must be at least 1")
        Integer farmId,

        @NotNull(message = "blockCode is required")
        @Pattern(regexp = "^\\s*[A-Za-z0-9]{1,10}\\s*$",
                message = "blockCode must be a short code such as A or B2, without prefix or space")
        String blockCode,

        @NotNull(message = "plannedDate is required")
        @FutureOrPresent(message = "plannedDate must be today or in the future")
        LocalDate plannedDate,

        @NotNull(message = "plannedQuantity is required")
        @Positive(message = "plannedQuantity must be greater than 0, with at most 3 decimals")
        @Digits(integer = 9, fraction = 3, message = "plannedQuantity must be greater than 0, with at most 3 decimals")
        Double plannedQuantity,

        @Size(max = 255, message = "note must be at most 255 characters")
        String note) {
}
