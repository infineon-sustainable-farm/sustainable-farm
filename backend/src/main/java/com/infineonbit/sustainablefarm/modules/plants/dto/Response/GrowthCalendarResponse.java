package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * API representation of a growth calendar entry.
 *
 * <p>Like {@link VarietyResponse}, the component names are English camelCase and
 * are the contract other modules consume, while the physical columns of
 * {@code calendrier_croissance} keep their French snake_case names.
 *
 * <p>These components are not columns of {@code calendrier_croissance}:
 * <ul>
 *     <li>{@code varieties} — names of the varieties recorded in {@code varietes}
 *         for the same farm and block. Empty when the block has none; every
 *         variety is listed when it has several, none is picked.</li>
 *     <li>{@code ageYears} and {@code ageMonths} — tree age in completed years
 *         and remaining months, computed from {@code plantingDate} at read time.</li>
 *     <li>{@code growthPhase} — growth phase derived from that age.</li>
 *     <li>{@code phaseYearsBand} — year band of that phase ("0–2 yrs",
 *         "3–5 yrs" or "6+ yrs"). Sent so clients never hold the thresholds.</li>
 * </ul>
 * All computed components are {@code null} when {@code plantingDate} is
 * {@code null}.
 */
public record GrowthCalendarResponse(
        Long id,
        Integer farmId,
        String blockCode,
        List<String> varieties,
        LocalDate plantingDate,
        String datePrecision,
        Integer ageYears,
        Integer ageMonths,
        String growthPhase,
        String phaseYearsBand,
        String currentStage,
        String phaseYears,
        Double localRainfallMm,
        String source,
        Instant lastUpdated) {
}
