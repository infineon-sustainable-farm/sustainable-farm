package com.infineonbit.sustainablefarm.modules.plants.entity;

/**
 * Health category of a block, derived from the health score of an inspection.
 *
 * <p>Computed on every read from the score and never stored. The score bands
 * are written once, in {@code HealthCalculator}.
 */
public enum HealthCategory {
    SEVERE_STRESS,
    WEAKENED,
    MODERATE,
    HEALTHY,
    VERY_HEALTHY
}
