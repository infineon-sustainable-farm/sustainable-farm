package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import java.time.Instant;

/**
 * Share of the full-production yield that a tree gives in one growth phase,
 * as the yield forecast and the plant alerts use it.
 *
 * <p>Every phase has a share: its default, sourced, until the user corrects
 * it. {@code yieldShare} and {@code source} are the values in effect;
 * {@code defaultYieldShare} and {@code defaultSource} stay beside them, so a
 * correction never hides what it replaced.
 *
 * @param code              the value a correction sends in its path, such as
 *                          {@code "GRADUAL_PRODUCTION"}
 * @param growthPhase       the phase as the forecast names it, such as
 *                          {@code "gradual production"}
 * @param yearsBand         its band of completed years, such as {@code "3–5 yrs"}
 * @param yieldShare        the share in effect, from 0 (no harvest) to 1 (the
 *                          full yield of the variety)
 * @param source            the source of the share in effect
 * @param corrected         {@code true} when the user corrected the default
 * @param defaultYieldShare the default share of the phase
 * @param defaultSource     the source of the default share
 * @param lastUpdated       when the correction was entered, {@code null} for a default
 */
public record GrowthPhaseYieldShareResponse(
        String code,
        String growthPhase,
        String yearsBand,
        Double yieldShare,
        String source,
        boolean corrected,
        Double defaultYieldShare,
        String defaultSource,
        Instant lastUpdated) {
}
