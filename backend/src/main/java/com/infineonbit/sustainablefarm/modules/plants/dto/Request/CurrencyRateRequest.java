package com.infineonbit.sustainablefarm.modules.plants.dto.Request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request to enter or replace the EUR to XOF rate: how many FCFA one euro is
 * worth.
 *
 * <p>Validated like {@link PurchaseRequest}: a 400 whose {@code fieldErrors}
 * lists each failing component. The rate fits the {@code currency_rate.rate}
 * column, 6 digits before the point and 6 after; its two constraints can fail
 * on the same value, such as -0.1234567, so they share one message.
 *
 * <p>{@code source} is optional and trimmed by the service; missing or blank,
 * the rate is recorded as a {@code user_entry}.
 */
public record CurrencyRateRequest(
        @Schema(description = "How many FCFA one euro is worth", example = "655.957")
        @NotNull(message = "rate is required")
        @Positive(message = "rate must be greater than 0 and less than 1000000, with at most 6 decimals")
        @Digits(integer = 6, fraction = 6,
                message = "rate must be greater than 0 and less than 1000000, with at most 6 decimals")
        Double rate,
        @Schema(description = "Where the rate comes from; user_entry when omitted", example = "BCEAO_fixed_parity_1999")
        @Size(max = 255, message = "source must be at most 255 characters")
        String source) {
}
