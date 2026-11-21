package com.infineonbit.sustainablefarm.modules.plants.entity;

/**
 * State of a {@link FertilizerPlan}.
 *
 * <p>A planned application starts PLANNED. It becomes DONE when the real
 * application is recorded, or CANCELLED when the farmer gives it up; both are
 * final. Being late is not a state: it is computed on every read from the
 * planned date.
 *
 * <p>Stored as text with a CHECK constraint (V1__baseline.sql): a new value
 * needs a migration that widens it.
 */
public enum FertilizerPlanStatus {
    PLANNED,
    DONE,
    CANCELLED
}
