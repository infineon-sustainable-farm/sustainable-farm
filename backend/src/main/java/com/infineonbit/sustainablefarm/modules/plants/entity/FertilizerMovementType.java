package com.infineonbit.sustainablefarm.modules.plants.entity;

/**
 * Kind of change recorded by a {@link FertilizerMovement}.
 *
 * <p>PURCHASE and ADJUSTMENT_IN add to the stock; APPLICATION, LOSS and
 * ADJUSTMENT_OUT take from it. The two adjustments correct the stock after a
 * physical count. No route writes them yet: they are declared already because
 * the CHECK constraint of the column is not widened by {@code ddl-auto=update}.
 */
public enum FertilizerMovementType {
    PURCHASE,
    APPLICATION,
    LOSS,
    ADJUSTMENT_IN,
    ADJUSTMENT_OUT
}
