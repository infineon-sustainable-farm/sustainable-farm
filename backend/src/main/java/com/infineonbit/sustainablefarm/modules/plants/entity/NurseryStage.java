package com.infineonbit.sustainablefarm.modules.plants.entity;

/**
 * Stage of the young plants of a {@link NurseryBatch}, from the seed to the
 * orchard.
 *
 * <p>No order is imposed: a graft that does not take sends the batch back from
 * GRAFTED to ROOTSTOCK_GROWTH until it is grafted again. The current stage of a
 * batch is that of its latest stage change, computed on every read and never
 * stored.
 *
 * <p>Every value is declared from the start: the CHECK constraint of the column
 * is not widened by {@code ddl-auto=update}.
 */
public enum NurseryStage {
    /** Seeds in the seedbed. */
    GERMINATION,
    /** Rootstock growing until it can be grafted. */
    ROOTSTOCK_GROWTH,
    /** The variety is grafted on the rootstock. */
    GRAFTED,
    /** Grafted plants getting used to the sun before they leave the nursery. */
    HARDENING,
    /** Ready to be planted in the orchard. */
    READY_TO_TRANSPLANT
}
