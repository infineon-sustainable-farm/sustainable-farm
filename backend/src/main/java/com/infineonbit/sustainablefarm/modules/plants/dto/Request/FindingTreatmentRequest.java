package com.infineonbit.sustainablefarm.modules.plants.dto.Request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request to record a treatment that answers a finding, the finding being the
 * one of the URL.
 *
 * <p>The farm, the block and the targeted issue are not sent: they are copied
 * from the finding. Validated like {@link PurchaseRequest}: a 400 whose
 * {@code fieldErrors} lists each failing component. The texts are trimmed by
 * the service, the unit trimmed and upper-cased; the unit is a string checked
 * by a pattern, not an enum, for the same reason as the unit of a fertilizer.
 *
 * <p>{@code preHarvestIntervalDays} is read as a decimal number so that 1.5 is
 * refused rather than silently cut to 1. The quantity and the interval each
 * have two constraints that can fail on the same value; they share one message,
 * so the reported message does not depend on the order of the violations.
 */
public record FindingTreatmentRequest(
        @NotNull(message = "treatedOn is required")
        @PastOrPresent(message = "treatedOn must be today or in the past")
        LocalDate treatedOn,

        @Schema(description = "Trade name of the product")
        @NotBlank(message = "productName is required")
        @Size(max = 255, message = "productName must be at most 255 characters")
        String productName,

        @NotBlank(message = "activeIngredient is required")
        @Size(max = 255, message = "activeIngredient must be at most 255 characters")
        String activeIngredient,

        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be greater than 0, with at most 3 decimals")
        @Digits(integer = 9, fraction = 3, message = "quantity must be greater than 0, with at most 3 decimals")
        Double quantity,

        @Schema(allowableValues = {"KG", "L"}, description = "Case and surrounding spaces are ignored")
        @NotNull(message = "unit is required")
        @Pattern(regexp = "^\\s*(KG|L)\\s*$", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "unit must be KG or L")
        String unit,

        @Schema(description = "Whole number of days, 0 or more", example = "14")
        @NotNull(message = "preHarvestIntervalDays is required")
        @PositiveOrZero(message = "preHarvestIntervalDays must be a whole number, 0 or more")
        @Digits(integer = 9, fraction = 0, message = "preHarvestIntervalDays must be a whole number, 0 or more")
        Double preHarvestIntervalDays,

        @NotBlank(message = "applicator is required")
        @Size(max = 255, message = "applicator must be at most 255 characters")
        String applicator,

        @Size(max = 255, message = "equipment must be at most 255 characters")
        String equipment) {
}
