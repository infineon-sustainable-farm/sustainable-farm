package com.infineonbit.sustainablefarm.modules.plants.entity;

/**
 * Status of a {@link HealthFinding}, derived from its treatments and its
 * resolution.
 *
 * <p>Computed on every read and never stored: the treatments and the
 * resolution are the recorded events, the status is their result.
 */
public enum HealthFindingStatus {
    /** No treatment, not resolved. */
    UNTREATED,
    /** At least one treatment, not resolved yet. */
    IN_PROGRESS,
    /** At least one treatment, then resolved. */
    TREATED,
    /** Resolved without any treatment. */
    CLOSED_WITHOUT_TREATMENT
}
