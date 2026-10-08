package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import java.time.Instant;

/**
 * API representation of an exchange rate: one {@code baseCurrency} is worth
 * {@code rate} {@code quoteCurrency}, for example 1 EUR = 655.957 XOF.
 *
 * <p>{@code source} says where the rate comes from, {@code user_entry} when it
 * was entered without one. {@code lastUpdated} is when it was entered or last
 * replaced.
 */
public record CurrencyRateResponse(
        String baseCurrency,
        String quoteCurrency,
        Double rate,
        String source,
        Instant lastUpdated) {
}
