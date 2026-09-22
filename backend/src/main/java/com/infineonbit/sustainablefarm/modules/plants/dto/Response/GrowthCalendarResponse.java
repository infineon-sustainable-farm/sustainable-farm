package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * API representation of a growth calendar entry.
 *
 * <p>Like {@link VarietyResponse}, the component names keep the snake_case domain
 * names: they are part of the contract other modules consume.
 *
 * <p>These components are not columns of {@code calendrier_croissance}:
 * <ul>
 *     <li>{@code varietes} — names of the varieties recorded in {@code varietes}
 *         for the same farm and block. Empty when the block has none; every
 *         variety is listed when it has several, none is picked.</li>
 *     <li>{@code age_annees} and {@code age_mois} — tree age in completed years
 *         and remaining months, computed from {@code date_plantation} at read time.</li>
 *     <li>{@code phase_croissance} — growth phase derived from that age.</li>
 *     <li>{@code phase_tranche_annees} — year band of that phase ("0–2 yrs",
 *         "3–5 yrs" or "6+ yrs"). Sent so clients never hold the thresholds.</li>
 * </ul>
 * All computed components are {@code null} when {@code date_plantation} is
 * {@code null}.
 */
public record GrowthCalendarResponse(
        Long id,
        Integer id_ferme,
        String bloc_parcelle,
        List<String> varietes,
        LocalDate date_plantation,
        String precision_date,
        Integer age_annees,
        Integer age_mois,
        String phase_croissance,
        String phase_tranche_annees,
        String stade_actuel,
        String phase_annees,
        Double pluviometrie_locale_mm,
        String source,
        Instant date_maj) {
}
