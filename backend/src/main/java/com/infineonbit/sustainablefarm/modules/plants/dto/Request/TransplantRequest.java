package com.infineonbit.sustainablefarm.modules.plants.dto.Request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/**
 * Request to transplant plants of a nursery batch into the orchard.
 *
 * <p>Validated like {@link NurseryBatchRequest}: a 400 whose
 * {@code fieldErrors} lists each failing component. The transplant records a
 * planting with the block, the date and the quantity, and the planting service
 * does not validate what it is given, so they follow the rules of
 * {@link PlantingRequest}: the block is a short code, normalized by the service
 * ({@code " e "} becomes {@code "E"}), and the date cannot be in the future.
 * The quantity is read as a decimal number so that 38.5 is refused rather than
 * cut to 38; its constraints share one message.
 *
 * <p>The service refuses, with a 422, a transplant dated before the start of
 * the batch or larger than the plants left in it. The stage of the batch is not
 * checked.
 */
public record TransplantRequest(
        @NotNull(message = "transplantedOn is required")
        @PastOrPresent(message = "transplantedOn must be today or in the past")
        LocalDate transplantedOn,

        @Schema(description = "Whole number, 1 or more", example = "100")
        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be a whole number, 1 or more")
        @Digits(integer = 9, fraction = 0, message = "quantity must be a whole number, 1 or more")
        Double quantity,

        @NotNull(message = "blockCode is required")
        @Pattern(regexp = "^\\s*[A-Za-z0-9]{1,10}\\s*$",
                message = "blockCode must be a short code such as A or B2, without prefix or space")
        String blockCode) {
}
