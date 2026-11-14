package com.infineonbit.sustainablefarm.modules.plants.dto.Request;

import com.infineonbit.sustainablefarm.modules.plants.validation.MeasuredValueMatchesMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request to record the maturity seen in the field on a variety planted on a
 * block of the farm.
 *
 * <p>Validated like {@link HarvestRequest}: a 400 whose {@code fieldErrors}
 * lists each failing component. Values are normalized by the service: the block
 * code is trimmed and upper-cased ({@code " b "} becomes {@code "B"}), the
 * variety name and the observer are trimmed, the status and the method trimmed
 * and upper-cased.
 *
 * <p>The status and the method are read as text, so that an unknown value is
 * answered with the listed values rather than with the raw text of the JSON
 * reader. {@code HARVESTED} is not a status of an observation.
 *
 * <p>{@code measuredValue} has no constraint of its own: every rule on it is
 * checked by {@link MeasuredValueMatchesMethod}, which reports one message at
 * most, since only one message per field is kept.
 */
@MeasuredValueMatchesMethod
public record MaturityObservationRequest(
        @Min(value = 1, message = "farmId must be at least 1")
        Integer farmId,

        @NotNull(message = "blockCode is required")
        @Pattern(regexp = "^\\s*[A-Za-z0-9]{1,10}\\s*$",
                message = "blockCode must be a short code such as A or B2, without prefix or space")
        String blockCode,

        @NotBlank(message = "varietyName is required")
        @Size(max = 255, message = "varietyName must be at most 255 characters")
        String varietyName,

        @NotNull(message = "observedOn is required")
        @PastOrPresent(message = "observedOn must be today or in the past")
        LocalDate observedOn,

        @Schema(allowableValues = {"IMMATURE", "RIPE", "READY_FOR_HARVEST"},
                description = "Case and surrounding spaces are ignored")
        @NotNull(message = "maturityStatus is required")
        @Pattern(regexp = "^\\s*(IMMATURE|RIPE|READY_FOR_HARVEST)\\s*$", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "maturityStatus must be IMMATURE, RIPE or READY_FOR_HARVEST")
        String maturityStatus,

        @Schema(allowableValues = {"FLESH_COLOR", "BRIX", "DRY_MATTER", "VISUAL"},
                description = "Case and surrounding spaces are ignored")
        @NotNull(message = "method is required")
        @Pattern(regexp = "^\\s*(FLESH_COLOR|BRIX|DRY_MATTER|VISUAL)\\s*$", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "method must be FLESH_COLOR, BRIX, DRY_MATTER or VISUAL")
        String method,

        @Schema(description = "In °Bx for BRIX, in percent for DRY_MATTER; left out for the other methods. "
                + "Above 0 and at most 100, with one decimal at most", example = "14.2")
        Double measuredValue,

        @Size(max = 255, message = "observer must be at most 255 characters")
        String observer) {
}
