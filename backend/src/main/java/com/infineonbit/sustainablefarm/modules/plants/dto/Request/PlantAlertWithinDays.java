package com.infineonbit.sustainablefarm.modules.plants.dto.Request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * How many days ahead the plant alerts announce a harvest season, bound from
 * the {@code withinDays} query parameter.
 *
 * <p>A record validated with {@code @Valid}, as {@link YieldForecastMonths}:
 * the core handler answers a failed record with a 400 whose
 * {@code fieldErrors} names {@code withinDays}, where a constrained
 * {@code @RequestParam} would give a 500. A value that is not a number, such
 * as {@code withinDays=abc}, is also a 400, whose {@code fieldErrors.withinDays}
 * holds the conversion message of Spring.
 *
 * <p>30 days when the parameter is missing: a default, which no source sets.
 * At most 90 days, the three months before the harvest during which the
 * European rule asks for inspections.
 *
 * @param withinDays number of days, from 1 to 90, or {@code null} for the default
 */
public record PlantAlertWithinDays(
        @Min(value = 1, message = "withinDays must be between 1 and 90")
        @Max(value = 90, message = "withinDays must be between 1 and 90")
        Integer withinDays) {
}
