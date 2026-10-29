package com.infineonbit.sustainablefarm.modules.plants.entity;

/**
 * Kind of a plant alert.
 *
 * <p>Computed on every read and never stored, like {@link HealthFindingStatus}.
 * Between two alerts of the same severity and the same date, the declaration
 * order below decides.
 */
public enum PlantAlertType {
    /** The harvest season of a planted variety starts soon, or is in progress. */
    HARVEST_APPROACHING,
    /** A treated block cannot be harvested yet: the pre-harvest interval of a treatment is not over. */
    PRE_HARVEST_INTERVAL,
    /** A fertilizer is at or below its alert threshold, as for the "Low stock" badge. */
    LOW_STOCK,
    /** A problem seen during an inspection is not resolved. */
    OPEN_HEALTH_ISSUE,
    /** A nursery batch is ready to transplant, or its planned transplant date has come. */
    NURSERY_READY
}
