package com.infineonbit.sustainablefarm.modules.plants.entity;

/**
 * How a {@link HealthInspection} was made: by looking at the trees, or by
 * reading traps, as for fruit flies.
 *
 * <p>Stored as text with a CHECK constraint, which {@code ddl-auto=update} does
 * not widen later: every planned value is declared here from the start.
 */
public enum InspectionMethod {
    VISUAL,
    TRAP
}
