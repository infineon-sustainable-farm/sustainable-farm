package com.infineonbit.sustainablefarm.modules.plants.dto.Request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Number of months of the yield forecast, bound from the {@code months} query
 * parameter.
 *
 * <p>It is a record validated with {@code @Valid}, not a constrained
 * {@code @RequestParam}. The core handler answers a failed record with a 400
 * whose {@code fieldErrors} names {@code months}. A constraint on a
 * {@code @RequestParam} raises another exception, which the core handler does
 * not map and answers with a 500.
 *
 * <p>A value that is not a number, such as {@code months=abc}, is also a 400,
 * but its {@code fieldErrors.months} holds the conversion message of Spring.
 *
 * @param months number of months, from 1 to 24, or {@code null} for the default
 */
public record YieldForecastMonths(
        @Min(value = 1, message = "months must be between 1 and 24")
        @Max(value = 24, message = "months must be between 1 and 24")
        Integer months) {
}
