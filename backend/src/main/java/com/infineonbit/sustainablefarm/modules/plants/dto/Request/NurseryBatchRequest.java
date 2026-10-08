package com.infineonbit.sustainablefarm.modules.plants.dto.Request;

import com.infineonbit.sustainablefarm.modules.plants.validation.SupplierMatchesOrigin;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request to start a batch of young plants in the nursery.
 *
 * <p>Validated like {@link PlantingRequest}: a 400 whose {@code fieldErrors}
 * lists each failing component. Values are normalized by the service: the
 * batch code is trimmed and upper-cased ({@code " p1 "} becomes {@code "P1"}),
 * the origin and the stage likewise, the texts are trimmed, and the planned
 * block follows the rule of a planting's block. A blank lot number or planned
 * block counts as none.
 *
 * <p>{@code origin} and {@code initialStage} are strings checked by a pattern,
 * not enums, so an unknown value comes back with {@code fieldErrors}, as for a
 * fertilizer. {@link SupplierMatchesOrigin} checks the supplier and its lot
 * number against the origin, lengths included.
 *
 * <p>{@code initialCount} is read as a decimal number so that 150.5 is refused
 * rather than silently cut to 150. Its constraints can fail on the same value
 * and share one message.
 *
 * <p>The planned transplant date may be in the future. The service refuses it
 * when it comes before {@code startedOn}.
 */
@SupplierMatchesOrigin
public record NurseryBatchRequest(
        @Min(value = 1, message = "farmId must be at least 1")
        Integer farmId,

        @Schema(description = "Up to 20 letters, digits or hyphens. Case and surrounding spaces are ignored",
                example = "P1")
        @NotNull(message = "batchCode is required")
        @Pattern(regexp = "^\\s*[A-Za-z0-9-]{1,20}\\s*$",
                message = "batchCode must be up to 20 letters, digits or hyphens, such as P1 or KEITT-2026-01")
        String batchCode,

        @Schema(description = "Variety grafted or to be grafted; a transplant plants this variety", example = "Keitt")
        @NotBlank(message = "varietyName is required")
        @Size(max = 255, message = "varietyName must be at most 255 characters")
        String varietyName,

        @Schema(allowableValues = {"IN_HOUSE", "PURCHASED"}, description = "Case and surrounding spaces are ignored")
        @NotNull(message = "origin is required")
        @Pattern(regexp = "^\\s*(IN_HOUSE|PURCHASED)\\s*$", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "origin must be IN_HOUSE or PURCHASED")
        String origin,

        @Schema(description = "Required when origin is PURCHASED and refused otherwise, at most 255 characters")
        String supplier,

        @Schema(description = "Optional, and refused unless origin is PURCHASED, at most 50 characters")
        String supplierLotNumber,

        @Schema(description = "Day of the sowing, or of the reception for a purchased batch")
        @NotNull(message = "startedOn is required")
        @PastOrPresent(message = "startedOn must be today or in the past")
        LocalDate startedOn,

        @Schema(description = "Whole number, 1 or more", example = "150")
        @NotNull(message = "initialCount is required")
        @Positive(message = "initialCount must be a whole number, 1 or more")
        @Digits(integer = 9, fraction = 0, message = "initialCount must be a whole number, 1 or more")
        Double initialCount,

        @Schema(allowableValues = {"GERMINATION", "ROOTSTOCK_GROWTH", "GRAFTED", "HARDENING", "READY_TO_TRANSPLANT"},
                description = "Case and surrounding spaces are ignored")
        @NotNull(message = "initialStage is required")
        @Pattern(regexp = "^\\s*(GERMINATION|ROOTSTOCK_GROWTH|GRAFTED|HARDENING|READY_TO_TRANSPLANT)\\s*$",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "initialStage must be GERMINATION, ROOTSTOCK_GROWTH, GRAFTED, HARDENING or "
                        + "READY_TO_TRANSPLANT")
        String initialStage,

        @Schema(description = "May be in the future, but not before startedOn")
        @NotNull(message = "plannedTransplantOn is required")
        LocalDate plannedTransplantOn,

        @Schema(description = "Optional; a short code such as A or B2, as for a planting")
        @Pattern(regexp = "^\\s*([A-Za-z0-9]{1,10})?\\s*$",
                message = "plannedBlockCode must be a short code such as A or B2, without prefix or space")
        String plannedBlockCode) {
}
