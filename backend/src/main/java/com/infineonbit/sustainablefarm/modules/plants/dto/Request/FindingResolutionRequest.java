package com.infineonbit.sustainablefarm.modules.plants.dto.Request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request to resolve a finding: the day it was seen to be settled, and an
 * optional note.
 *
 * <p>Validated like {@link PlantingRequest}: a 400 whose {@code fieldErrors}
 * lists each failing component. The note is trimmed by the service, and a blank
 * note counts as none.
 */
public record FindingResolutionRequest(
        @NotNull(message = "resolvedOn is required")
        @PastOrPresent(message = "resolvedOn must be today or in the past")
        LocalDate resolvedOn,

        @Size(max = 255, message = "note must be at most 255 characters")
        String note) {
}
