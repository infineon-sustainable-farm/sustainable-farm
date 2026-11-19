package com.infineonbit.sustainablefarm.modules.plants.dto.Request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request to correct the yield share of a growth phase.
 *
 * <p>Validated like {@link CurrencyRateRequest}: a 400 whose {@code fieldErrors}
 * lists each failing component. The share runs from 0 (no harvest) to 1 (the
 * full yield of the variety), the bounds of the {@code growth_phase_yield_share}
 * check, with at most 3 decimals; its three constraints can fail on the same
 * value, such as -0.1234, so they share one message.
 *
 * <p>{@code source} is optional and trimmed by the service; missing or blank,
 * the share is recorded as a {@code user_entry}.
 */
public record GrowthPhaseYieldShareRequest(
        @Schema(description = "Share of the full-production yield, from 0 to 1", example = "0.3")
        @NotNull(message = "yieldShare is required")
        @DecimalMin(value = "0", message = "yieldShare must be between 0 and 1, with at most 3 decimals")
        @DecimalMax(value = "1", message = "yieldShare must be between 0 and 1, with at most 3 decimals")
        @Digits(integer = 1, fraction = 3, message = "yieldShare must be between 0 and 1, with at most 3 decimals")
        Double yieldShare,
        @Schema(description = "Where the share comes from; user_entry when omitted", example = "agronomist_visit_2026")
        @Size(max = 255, message = "source must be at most 255 characters")
        String source) {
}
