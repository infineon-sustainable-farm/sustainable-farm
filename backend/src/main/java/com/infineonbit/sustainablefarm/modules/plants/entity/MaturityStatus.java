package com.infineonbit.sustainablefarm.modules.plants.entity;

/**
 * Maturity of the fruit of a planted variety on a day, in the words of the
 * data requirements (PL-05-01): immature, ripe, ready for harvest.
 *
 * <p>Estimated on every read, from the harvest window of the variety and the
 * pre-harvest intervals of its block, with the rules of the plant alerts; the
 * comments of the values give those rules. A {@link MaturityObservation}
 * stores the status seen in the field instead, which takes precedence over
 * the estimate for the season it was made for.
 *
 * <p>Stored by the observations as text with a CHECK constraint, which
 * {@code ddl-auto=update} does not widen later: a value added here would be
 * refused by every existing PostgreSQL database until its constraint is
 * changed by hand.
 */
public enum MaturityStatus {
    /** The harvest window opens later than the look-ahead of the alerts, or never. */
    IMMATURE,
    /** The harvest window opens within the look-ahead, or it is open but a pre-harvest interval still runs. */
    RIPE,
    /** The harvest window is open and no pre-harvest interval runs on the block. */
    READY_FOR_HARVEST
}
