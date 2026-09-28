package com.infineonbit.sustainablefarm.modules.plants.dto.Request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Request to record an inspection of a block, with the problems seen.
 *
 * <p>Validated like {@link PlantingRequest}: a 400 whose {@code fieldErrors}
 * lists each failing component, a finding's under its index, for example
 * {@code findings[1].issueCode}. The block code has the same {@code @NotNull}
 * and pattern as a planting, and is normalized the same way by the service
 * ({@code " c "} becomes {@code "C"}); the observer is trimmed, the method
 * trimmed and upper-cased.
 *
 * <p>{@code findings} is required: an empty list records a visit with nothing
 * found, which is worth keeping. A null item is refused.
 *
 * <p>{@code healthScorePct} is read as a decimal number so that a value such
 * as 48.5 is refused rather than silently cut to 48, which is what happens to
 * a decimal sent into an integer. Its three constraints can fail on the same
 * value and share one message, so the reported message does not depend on the
 * order of the violations.
 */
public record HealthInspectionRequest(
        @Min(value = 1, message = "farmId must be at least 1")
        Integer farmId,

        @NotNull(message = "blockCode is required")
        @Pattern(regexp = "^\\s*[A-Za-z0-9]{1,10}\\s*$",
                message = "blockCode must be a short code such as A or B2, without prefix or space")
        String blockCode,

        @NotNull(message = "inspectedOn is required")
        @PastOrPresent(message = "inspectedOn must be today or in the past")
        LocalDate inspectedOn,

        @Schema(description = "Whole number from 0 to 100", example = "48")
        @NotNull(message = "healthScorePct is required")
        @DecimalMin(value = "0", message = "healthScorePct must be a whole number between 0 and 100")
        @DecimalMax(value = "100", message = "healthScorePct must be a whole number between 0 and 100")
        @Digits(integer = 3, fraction = 0, message = "healthScorePct must be a whole number between 0 and 100")
        Double healthScorePct,

        @Size(max = 255, message = "observer must be at most 255 characters")
        String observer,

        @Schema(allowableValues = {"VISUAL", "TRAP"}, description = "Case and surrounding spaces are ignored")
        @Pattern(regexp = "^\\s*(VISUAL|TRAP)\\s*$", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "method must be VISUAL or TRAP")
        String method,

        @NotNull(message = "findings is required")
        List<@NotNull(message = "findings must not contain null") @Valid HealthFindingRequest> findings) {
}
