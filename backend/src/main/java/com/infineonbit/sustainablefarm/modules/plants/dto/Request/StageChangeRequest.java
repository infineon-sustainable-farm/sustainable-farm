package com.infineonbit.sustainablefarm.modules.plants.dto.Request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

/**
 * Request to record that a nursery batch reached a stage.
 *
 * <p>Validated like {@link NurseryBatchRequest}: a 400 whose
 * {@code fieldErrors} lists each failing component. The stage is a string
 * checked by a pattern, in any case and with or without surrounding spaces;
 * the service trims and upper-cases it.
 *
 * <p>Any stage can follow any other. The service refuses, with a 422, a date
 * before the start of the batch or before its last stage change, and the stage
 * the batch is already at.
 */
public record StageChangeRequest(
        @Schema(allowableValues = {"GERMINATION", "ROOTSTOCK_GROWTH", "GRAFTED", "HARDENING", "READY_TO_TRANSPLANT"},
                description = "Case and surrounding spaces are ignored")
        @NotNull(message = "stage is required")
        @Pattern(regexp = "^\\s*(GERMINATION|ROOTSTOCK_GROWTH|GRAFTED|HARDENING|READY_TO_TRANSPLANT)\\s*$",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "stage must be GERMINATION, ROOTSTOCK_GROWTH, GRAFTED, HARDENING or READY_TO_TRANSPLANT")
        String stage,

        @NotNull(message = "changedOn is required")
        @PastOrPresent(message = "changedOn must be today or in the past")
        LocalDate changedOn) {
}
