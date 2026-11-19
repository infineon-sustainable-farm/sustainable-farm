package com.infineonbit.sustainablefarm.modules.plants.dto.Request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Request to add a fertilizer to the catalogue.
 *
 * <p>Validation errors come back as a 400 whose {@code fieldErrors} lists each
 * failing component. Values are normalized by the service: the name and the
 * composition are trimmed, and the type and unit are trimmed and upper-cased
 * ({@code " kg "} becomes {@code KG}).
 *
 * <p>{@code fertilizerType} and {@code unit} are strings checked by a pattern,
 * not enums. An unknown enum value fails while the JSON body is read, before
 * any validation, and comes back as a 400 without {@code fieldErrors} holding
 * the reader's own text. Like the block code of a planting, each has
 * {@code @NotNull} rather than {@code @NotBlank}: a blank value already fails
 * the pattern.
 *
 * <p>{@code reorderThreshold} has two constraints that can fail on the same
 * value, such as -0.1234. They share one message, so the reported message does
 * not depend on the order of the violations.
 */
public record FertilizerRequest(
        @NotBlank(message = "name is required")
        @Size(max = 255, message = "name must be at most 255 characters")
        String name,

        @Schema(allowableValues = {"MINERAL", "ORGANIC"},
                description = "Case and surrounding spaces are ignored")
        @NotNull(message = "fertilizerType is required")
        @Pattern(regexp = "^\\s*(MINERAL|ORGANIC)\\s*$", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "fertilizerType must be MINERAL or ORGANIC")
        String fertilizerType,

        @Size(max = 100, message = "composition must be at most 100 characters")
        String composition,

        @Schema(allowableValues = {"KG", "L"},
                description = "Case and surrounding spaces are ignored")
        @NotNull(message = "unit is required")
        @Pattern(regexp = "^\\s*(KG|L)\\s*$", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "unit must be KG or L")
        String unit,

        @PositiveOrZero(message = "reorderThreshold must be 0 or more, with at most 3 decimals")
        @Digits(integer = 9, fraction = 3,
                message = "reorderThreshold must be 0 or more, with at most 3 decimals")
        Double reorderThreshold) {
}
