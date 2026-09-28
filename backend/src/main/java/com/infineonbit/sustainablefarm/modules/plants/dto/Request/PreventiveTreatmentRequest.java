package com.infineonbit.sustainablefarm.modules.plants.dto.Request;

import com.infineonbit.sustainablefarm.modules.plants.validation.OtherLabelMatchesIssue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request to record a preventive treatment: one that answers no finding, such
 * as a fruit fly bait put down before the attacks.
 *
 * <p>The treatment fields and their messages are those of
 * {@link FindingTreatmentRequest}. The block has the rule of a planting and is
 * normalized the same way. {@code targetIssueCode} is a code of the catalogue,
 * in any case; one with the right form but missing from the catalogue is
 * refused by the service with a 422. {@code targetOtherLabel} names the problem
 * when the code is {@code OTHER}, and only then, with the rule of a finding's
 * label, so that a treatment against a problem outside the catalogue can still
 * be recorded.
 */
@OtherLabelMatchesIssue(codeField = "targetIssueCode", labelField = "targetOtherLabel")
public record PreventiveTreatmentRequest(
        @Min(value = 1, message = "farmId must be at least 1")
        Integer farmId,

        @NotNull(message = "blockCode is required")
        @Pattern(regexp = "^\\s*[A-Za-z0-9]{1,10}\\s*$",
                message = "blockCode must be a short code such as A or B2, without prefix or space")
        String blockCode,

        @Schema(description = "Code of the catalogue, for example FRUIT_FLY. Case and surrounding spaces "
                + "are ignored")
        @NotNull(message = "targetIssueCode is required")
        @Pattern(regexp = "^\\s*[A-Za-z0-9_]{1,50}\\s*$",
                message = "targetIssueCode must be a code of letters, digits and underscores, such as FRUIT_FLY")
        String targetIssueCode,

        @Schema(description = "Name of the problem, required when targetIssueCode is OTHER and refused "
                + "otherwise, at most 60 characters")
        String targetOtherLabel,

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

        @Schema(description = "Whole number of days, 0 or more", example = "1")
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
