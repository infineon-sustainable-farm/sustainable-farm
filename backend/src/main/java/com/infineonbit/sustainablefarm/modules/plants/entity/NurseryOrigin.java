package com.infineonbit.sustainablefarm.modules.plants.entity;

/**
 * Where the plants of a {@link NurseryBatch} come from.
 *
 * <p>Both values are declared from the start: the CHECK constraint of the
 * column is not widened by {@code ddl-auto=update}.
 */
public enum NurseryOrigin {
    /** Seeds sown on the farm. */
    IN_HOUSE,
    /** Young plants bought from a nursery, whose name is then recorded. */
    PURCHASED
}
