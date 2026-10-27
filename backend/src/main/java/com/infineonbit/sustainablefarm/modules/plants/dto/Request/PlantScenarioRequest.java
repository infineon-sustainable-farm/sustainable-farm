package com.infineonbit.sustainablefarm.modules.plants.dto.Request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Request to enter or correct a planting scenario: the assumptions of a plan,
 * each with an optional source.
 *
 * <p>Validated like {@link VarietyReferenceRequest}: a 400 whose
 * {@code fieldErrors} lists each failing component. The yield per tree has the
 * bounds of the agronomic reference. A value with two constraints that can fail
 * together, such as -0.15, shares one message between them.
 *
 * <p>{@code lossRate}, {@code pricePerKg}, {@code variableCostPerKg} and
 * {@code costPerAdditionalTree} are optional: left out, they stay unknown and
 * the analysis says which results they hold back, rather than reading them as
 * 0. The source of an optional value left out is ignored.
 *
 * <p>Every amount is in {@code currency}, checked by a pattern as for a
 * fertilizer purchase. The service trims the texts and upper-cases the
 * currency. A source left out or blank is {@code user_entry} for a new value;
 * on a correction, it stays as it was when its value does not change.
 */
public record PlantScenarioRequest(
        @Schema(description = "Name of the scenario, unique ignoring case, accents and surrounding spaces",
                example = "Zalka 2025")
        @NotBlank(message = "name is required")
        @Size(max = 255, message = "name must be at most 255 characters")
        String name,

        @Schema(description = "Variety the plan is about, for information", example = "Keitt")
        @Size(max = 255, message = "varietyName must be at most 255 characters")
        String varietyName,

        @Schema(description = "Number of trees of the plan, all planted in its first year", example = "200")
        @NotNull(message = "treeCount is required")
        @Min(value = 1, message = "treeCount must be between 1 and 1000000")
        @Max(value = 1_000_000, message = "treeCount must be between 1 and 1000000")
        Integer treeCount,

        @Size(max = 255, message = "treeCountSource must be at most 255 characters")
        String treeCountSource,

        @Schema(description = "Yearly yield of one tree in full production, in kg", example = "220")
        @NotNull(message = "yieldPerTreeKg is required")
        @Positive(message = "yieldPerTreeKg must be greater than 0 and at most 1000, with at most 1 decimal")
        @DecimalMax(value = "1000",
                message = "yieldPerTreeKg must be greater than 0 and at most 1000, with at most 1 decimal")
        @Digits(integer = 4, fraction = 1,
                message = "yieldPerTreeKg must be greater than 0 and at most 1000, with at most 1 decimal")
        Double yieldPerTreeKg,

        @Size(max = 255, message = "yieldSource must be at most 255 characters")
        String yieldSource,

        @Schema(description = "Share of the harvest lost before sale, from 0 to 1; unknown when omitted",
                example = "0.15")
        @DecimalMin(value = "0", message = "lossRate must be between 0 and 1, with at most 3 decimals")
        @DecimalMax(value = "1", message = "lossRate must be between 0 and 1, with at most 3 decimals")
        @Digits(integer = 1, fraction = 3, message = "lossRate must be between 0 and 1, with at most 3 decimals")
        Double lossRate,

        @Size(max = 255, message = "lossRateSource must be at most 255 characters")
        String lossRateSource,

        @Schema(description = "Selling price of one kg of fresh mangoes; unknown when omitted", example = "0.9072")
        @Positive(message = "pricePerKg must be greater than 0, with at most 4 decimals")
        @Digits(integer = 10, fraction = 4, message = "pricePerKg must be greater than 0, with at most 4 decimals")
        Double pricePerKg,

        @Size(max = 255, message = "priceSource must be at most 255 characters")
        String priceSource,

        @Schema(allowableValues = {"XOF", "EUR"},
                description = "Currency of every amount. Case and surrounding spaces are ignored")
        @NotNull(message = "currency is required")
        @Pattern(regexp = "^\\s*(XOF|EUR)\\s*$", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "currency must be XOF or EUR")
        String currency,

        @Schema(description = "Operating cost of a year whatever the harvest, possibly 0", example = "6707")
        @NotNull(message = "annualFixedCost is required")
        @PositiveOrZero(message = "annualFixedCost must be 0 or more, with at most 2 decimals")
        @Digits(integer = 12, fraction = 2, message = "annualFixedCost must be 0 or more, with at most 2 decimals")
        Double annualFixedCost,

        @Size(max = 255, message = "annualFixedCostSource must be at most 255 characters")
        String annualFixedCostSource,

        @Schema(description = "Operating cost of each kg sold; unknown when omitted", example = "0.05")
        @PositiveOrZero(message = "variableCostPerKg must be 0 or more, with at most 4 decimals")
        @Digits(integer = 10, fraction = 4, message = "variableCostPerKg must be 0 or more, with at most 4 decimals")
        Double variableCostPerKg,

        @Size(max = 255, message = "variableCostSource must be at most 255 characters")
        String variableCostSource,

        @Schema(description = "Total investment of the plan, spent before its first year", example = "171512")
        @NotNull(message = "initialInvestment is required")
        @Positive(message = "initialInvestment must be greater than 0, with at most 2 decimals")
        @Digits(integer = 12, fraction = 2,
                message = "initialInvestment must be greater than 0, with at most 2 decimals")
        Double initialInvestment,

        @Size(max = 255, message = "investmentSource must be at most 255 characters")
        String investmentSource,

        @Schema(description = "Investment for each tree added by a variant; unknown when omitted", example = "12.5")
        @PositiveOrZero(message = "costPerAdditionalTree must be 0 or more, with at most 2 decimals")
        @Digits(integer = 12, fraction = 2,
                message = "costPerAdditionalTree must be 0 or more, with at most 2 decimals")
        Double costPerAdditionalTree,

        @Size(max = 255, message = "costPerAdditionalTreeSource must be at most 255 characters")
        String costPerAdditionalTreeSource) {
}
