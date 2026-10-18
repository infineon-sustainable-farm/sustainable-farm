package com.infineonbit.sustainablefarm.modules.plants.dto.Request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request to record an application of a fertilizer on a block, in the unit of
 * that fertilizer.
 *
 * <p>GLOBALG.A.P. asks, for each application, for the field, the date, the
 * fertilizer, the quantity and who applied it: the block, the date, the
 * fertilizer of the URL, the quantity and the applicator here. The block needs
 * no recorded planting, since the base fertilizer goes into the hole before the
 * tree.
 *
 * <p>Validated like {@link PlantingRequest}: a 400 whose {@code fieldErrors}
 * lists each failing component. The block code has the same {@code @NotNull}
 * and pattern, and is normalized the same way by the service ({@code " b "}
 * becomes {@code "B"}); the applicator and the method are trimmed. The quantity
 * has two constraints that share one message, as in {@link PurchaseRequest}.
 */
public record ApplicationRequest(
        @NotNull(message = "applicationDate is required")
        @PastOrPresent(message = "applicationDate must be today or in the past")
        LocalDate applicationDate,

        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be greater than 0, with at most 3 decimals")
        @Digits(integer = 9, fraction = 3, message = "quantity must be greater than 0, with at most 3 decimals")
        Double quantity,

        @Min(value = 1, message = "farmId must be at least 1")
        Integer farmId,

        @NotNull(message = "blockCode is required")
        @Pattern(regexp = "^\\s*[A-Za-z0-9]{1,10}\\s*$",
                message = "blockCode must be a short code such as A or B2, without prefix or space")
        String blockCode,

        @NotBlank(message = "applicator is required")
        @Size(max = 255, message = "applicator must be at most 255 characters")
        String applicator,

        @Size(max = 255, message = "method must be at most 255 characters")
        String method) {
}
