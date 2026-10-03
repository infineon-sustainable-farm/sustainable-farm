package com.infineonbit.sustainablefarm.modules.plants.dto.Request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request to record the planting of a variety on a block of the farm.
 *
 * <p>Validation errors come back as a 400 whose {@code fieldErrors} lists each
 * failing component. Values are normalized by the service, not here: the block
 * code is trimmed and upper-cased ({@code " a "} becomes {@code "A"}) and the
 * variety name is trimmed.
 *
 * <p>The block code has {@code @NotNull} rather than {@code @NotBlank}: a blank
 * value already fails the pattern. Two constraints failing on the same value
 * would make the reported message depend on the order of the violations, which
 * Bean Validation does not fix.
 */
public record PlantingRequest(
        @Min(value = 1, message = "farmId must be at least 1")
        Integer farmId,

        @NotNull(message = "blockCode is required")
        @Pattern(regexp = "^\\s*[A-Za-z0-9]{1,10}\\s*$",
                message = "blockCode must be a short code such as A or B2, without prefix or space")
        String blockCode,

        @NotBlank(message = "varietyName is required")
        @Size(max = 255, message = "varietyName must be at most 255 characters")
        String varietyName,

        @NotNull(message = "plantingDate is required")
        @PastOrPresent(message = "plantingDate must be today or in the past")
        LocalDate plantingDate,

        @NotNull(message = "treeCount is required")
        @Min(value = 1, message = "treeCount must be at least 1")
        Integer treeCount) {
}
