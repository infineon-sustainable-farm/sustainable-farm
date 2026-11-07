package com.infineonbit.sustainablefarm.modules.plants.dto.Request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * A variant of a scenario and the length of its projection, bound from the
 * query parameters of the analysis. Nothing of it is stored.
 *
 * <p>It is a record validated with {@code @Valid}, like
 * {@link YieldForecastMonths}, so a value out of bounds is a 400 whose
 * {@code fieldErrors} names it. The checks that need the stored scenario, such
 * as keeping at least one tree, are made by the service.
 *
 * <p>A variant exists as soon as one of its four first components is given; the
 * others then keep the value of the scenario.
 *
 * @param treeCountChange       trees added, or removed when negative
 * @param yieldPerTreeChangePct change of the yield per tree, in percent
 * @param lossRate              loss rate of the variant, from 0 to 1
 * @param additionalInvestment  investment of the variant on top of that of the
 *                              scenario and of its added trees
 * @param horizonYears          number of years of the projection, from 1 to 30,
 *                              or {@code null} for the default
 */
public record ScenarioVariantParams(
        @Schema(description = "Trees added, or removed when negative", example = "20")
        @Min(value = -1_000_000, message = "treeCountChange must be between -1000000 and 1000000")
        @Max(value = 1_000_000, message = "treeCountChange must be between -1000000 and 1000000")
        Integer treeCountChange,

        @Schema(description = "Change of the yield per tree, in percent", example = "10")
        @DecimalMin(value = "-100", inclusive = false,
                message = "yieldPerTreeChangePct must be above -100 and at most 1000, with at most 1 decimal")
        @DecimalMax(value = "1000",
                message = "yieldPerTreeChangePct must be above -100 and at most 1000, with at most 1 decimal")
        @Digits(integer = 4, fraction = 1,
                message = "yieldPerTreeChangePct must be above -100 and at most 1000, with at most 1 decimal")
        Double yieldPerTreeChangePct,

        @Schema(description = "Loss rate of the variant, from 0 to 1; needs the loss rate of the scenario",
                example = "0.1")
        @DecimalMin(value = "0", message = "lossRate must be between 0 and 1, with at most 3 decimals")
        @DecimalMax(value = "1", message = "lossRate must be between 0 and 1, with at most 3 decimals")
        @Digits(integer = 1, fraction = 3, message = "lossRate must be between 0 and 1, with at most 3 decimals")
        Double lossRate,

        @Schema(description = "Investment of the variant on top of the scenario's, in its currency", example = "5000")
        @PositiveOrZero(message = "additionalInvestment must be 0 or more, with at most 2 decimals")
        @Digits(integer = 12, fraction = 2,
                message = "additionalInvestment must be 0 or more, with at most 2 decimals")
        Double additionalInvestment,

        @Schema(description = "Number of years of the projection, 10 by default", example = "10")
        @Min(value = 1, message = "horizonYears must be between 1 and 30")
        @Max(value = 30, message = "horizonYears must be between 1 and 30")
        Integer horizonYears) {

    /** Whether a variant is asked for: one of the four first components is given. */
    public boolean hasVariant() {
        return treeCountChange != null || yieldPerTreeChangePct != null || lossRate != null
                || additionalInvestment != null;
    }
}
