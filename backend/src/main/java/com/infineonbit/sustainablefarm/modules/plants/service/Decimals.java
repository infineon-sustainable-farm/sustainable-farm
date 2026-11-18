package com.infineonbit.sustainablefarm.modules.plants.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Conversions between the quantities plants stores, in {@code BigDecimal} at the scale of their
 * column (docs/adr/0002), and the numbers its API reads and writes, in {@code Double}.
 *
 * <p>The API keeps its numbers as they were: a stored {@code 12.500} is sent as {@code 12.5}, the
 * value the client entered, never with the trailing zeros of the column.</p>
 */
public final class Decimals {

    private Decimals() {
    }

    /**
     * A number of the API as a stored quantity.
     *
     * @param value the number, possibly {@code null}
     * @param scale decimals of the column; a number with more decimals is rounded half up
     * @return the quantity at that scale, or {@code null}
     */
    public static BigDecimal of(Double value, int scale) {
        return value == null ? null : BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
    }

    /**
     * A stored quantity as a number of the API.
     *
     * @param value the quantity, possibly {@code null}
     * @return the number, or {@code null}
     */
    public static Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    /**
     * Whether a number of the API is the stored quantity, ignoring the scale: {@code 12.5} is
     * {@code 12.500}.
     *
     * @param value  the number, possibly {@code null}
     * @param stored the quantity, possibly {@code null}
     * @return {@code true} when both are {@code null} or equal in value
     */
    public static boolean sameValue(Double value, BigDecimal stored) {
        if (value == null || stored == null) {
            return value == null && stored == null;
        }
        return BigDecimal.valueOf(value).compareTo(stored) == 0;
    }
}
