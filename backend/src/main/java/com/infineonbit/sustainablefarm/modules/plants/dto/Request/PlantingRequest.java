package com.infineonbit.sustainablefarm.modules.plants.dto.Request;

import com.infineonbit.sustainablefarm.modules.plants.validation.SpacingComplete;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
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
 *
 * <p>The spacings and the vigor are optional, and go on the variety row. Each
 * spacing is above 0 and at most 30 m, with at most one decimal; its three
 * constraints can fail on the same value and share one message.
 * {@link SpacingComplete} asks for both spacings or none. {@code vigor} is a
 * string checked by a pattern, not an enum, so an unknown value comes back
 * with {@code fieldErrors}; the service trims and upper-cases it.
 */
@SpacingComplete
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
        Integer treeCount,

        @Schema(description = "Inter-row spacing in metres, above 0 and at most 30, one decimal at most. "
                + "Optional, but given with treeSpacingM", example = "8")
        @DecimalMin(value = "0", inclusive = false,
                message = "rowSpacingM must be above 0 and at most 30 m, with at most 1 decimal")
        @DecimalMax(value = "30",
                message = "rowSpacingM must be above 0 and at most 30 m, with at most 1 decimal")
        @Digits(integer = 2, fraction = 1,
                message = "rowSpacingM must be above 0 and at most 30 m, with at most 1 decimal")
        BigDecimal rowSpacingM,

        @Schema(description = "Intra-row spacing in metres, above 0 and at most 30, one decimal at most. "
                + "Optional, but given with rowSpacingM", example = "8")
        @DecimalMin(value = "0", inclusive = false,
                message = "treeSpacingM must be above 0 and at most 30 m, with at most 1 decimal")
        @DecimalMax(value = "30",
                message = "treeSpacingM must be above 0 and at most 30 m, with at most 1 decimal")
        @Digits(integer = 2, fraction = 1,
                message = "treeSpacingM must be above 0 and at most 30 m, with at most 1 decimal")
        BigDecimal treeSpacingM,

        @Schema(allowableValues = {"GOOD", "AVERAGE", "POOR"},
                description = "Optional. Case and surrounding spaces are ignored")
        @Pattern(regexp = "^\\s*(GOOD|AVERAGE|POOR)\\s*$", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "vigor must be GOOD, AVERAGE or POOR")
        String vigor) {

    /**
     * A planting without spacing or vigor, as a nursery transplant records it.
     */
    public PlantingRequest(Integer farmId, String blockCode, String varietyName, LocalDate plantingDate,
                           Integer treeCount) {
        this(farmId, blockCode, varietyName, plantingDate, treeCount, null, null, null);
    }
}
