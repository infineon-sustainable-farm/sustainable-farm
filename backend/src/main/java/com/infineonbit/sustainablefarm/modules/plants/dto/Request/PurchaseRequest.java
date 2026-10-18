package com.infineonbit.sustainablefarm.modules.plants.dto.Request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request to record a purchase of a fertilizer, in the unit of that fertilizer.
 *
 * <p>Validated like {@link FertilizerRequest}: a 400 whose {@code fieldErrors}
 * lists each failing component. The supplier is trimmed by the service, and the
 * currency trimmed and upper-cased. The currency is a string checked by a
 * pattern, not an enum, for the same reason as the unit of a fertilizer.
 *
 * <p>{@code totalCost} is optional. Given without a currency, it is in XOF, the
 * currency of the farm. A currency given without a cost is ignored.
 *
 * <p>The quantity and the cost each have two constraints that can fail on the
 * same value, such as -0.1234. They share one message, so the reported message
 * does not depend on the order of the violations.
 */
public record PurchaseRequest(
        @NotNull(message = "purchaseDate is required")
        @PastOrPresent(message = "purchaseDate must be today or in the past")
        LocalDate purchaseDate,

        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be greater than 0, with at most 3 decimals")
        @Digits(integer = 9, fraction = 3, message = "quantity must be greater than 0, with at most 3 decimals")
        Double quantity,

        @NotBlank(message = "supplier is required")
        @Size(max = 255, message = "supplier must be at most 255 characters")
        String supplier,

        @Positive(message = "totalCost must be greater than 0, with at most 2 decimals")
        @Digits(integer = 12, fraction = 2, message = "totalCost must be greater than 0, with at most 2 decimals")
        Double totalCost,

        @Schema(allowableValues = {"XOF", "EUR"},
                description = "Currency of totalCost, XOF when omitted. Case and surrounding spaces are ignored")
        @Pattern(regexp = "^\\s*(XOF|EUR)\\s*$", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "currency must be XOF or EUR")
        String currency) {
}
