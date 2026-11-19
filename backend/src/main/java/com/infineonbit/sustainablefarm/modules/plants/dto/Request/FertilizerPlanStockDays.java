package com.infineonbit.sustainablefarm.modules.plants.dto.Request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * How many days ahead the stock check of the planned applications looks,
 * bound from the {@code days} query parameter.
 *
 * <p>A record validated with {@code @Valid}, as {@link PlantAlertWithinDays}:
 * the core handler answers a failed record with a 400 whose
 * {@code fieldErrors} names {@code days}, where a constrained
 * {@code @RequestParam} would give a 500.
 *
 * <p>30 days when the parameter is missing: a default, which no source sets.
 * At most 365 days, a whole year of plans.
 *
 * @param days number of days, from 1 to 365, or {@code null} for the default
 */
public record FertilizerPlanStockDays(
        @Min(value = 1, message = "days must be between 1 and 365")
        @Max(value = 365, message = "days must be between 1 and 365")
        Integer days) {
}
