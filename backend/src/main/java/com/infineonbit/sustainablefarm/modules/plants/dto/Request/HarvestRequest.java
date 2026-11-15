package com.infineonbit.sustainablefarm.modules.plants.dto.Request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request to record a harvest of a variety planted on a block of the farm.
 *
 * <p>Validated like {@link PlantingRequest}: a 400 whose {@code fieldErrors}
 * lists each failing component. Values are normalized by the service: the block
 * code is trimmed and upper-cased ({@code " b "} becomes {@code "B"}) and the
 * variety name is trimmed.
 *
 * <p>The block code has the same {@code @NotNull} and pattern as
 * {@link PlantingRequest}, for the same reason: a blank value already fails the
 * pattern, and two constraints failing on the same value would make the
 * reported message depend on the order of the violations.
 */
public record HarvestRequest(
        @Min(value = 1, message = "farmId must be at least 1")
        Integer farmId,

        @NotNull(message = "blockCode is required")
        @Pattern(regexp = "^\\s*[A-Za-z0-9]{1,10}\\s*$",
                message = "blockCode must be a short code such as A or B2, without prefix or space")
        String blockCode,

        @NotBlank(message = "varietyName is required")
        @Size(max = 255, message = "varietyName must be at most 255 characters")
        String varietyName,

        @NotNull(message = "harvestDate is required")
        @PastOrPresent(message = "harvestDate must be today or in the past")
        LocalDate harvestDate,

        @NotNull(message = "quantityKg is required")
        @Positive(message = "quantityKg must be greater than 0")
        Double quantityKg) {
}
