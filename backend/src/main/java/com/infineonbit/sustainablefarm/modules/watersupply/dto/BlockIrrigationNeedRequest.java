package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Irrigation need of a Plants block. Only {@code blockCode} is required: without {@code growthPhase}
 * the phase is read from the Plants growth calendar; {@code date} defaults to today (UTC).
 * {@code treeCount} and {@code ageYears} are echoed back until a per-tree coefficient is validated.
 */
public record BlockIrrigationNeedRequest(
        @NotBlank String blockCode,
        UUID farmId,
        @PositiveOrZero Integer treeCount,
        String growthPhase,
        @PositiveOrZero Integer ageYears,
        LocalDate date) {
}
