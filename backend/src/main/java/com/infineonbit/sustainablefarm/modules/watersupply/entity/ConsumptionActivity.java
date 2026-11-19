package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Activities a water consumption can be attributed to (Product Transformation integration).
 *
 * <p>Stored as the constant name in {@code water_consumption.activity} ({@code @Enumerated(STRING)}):
 * the list must stay aligned with the activities Product Transformation actually runs (to confirm
 * with that module).</p>
 */
public enum ConsumptionActivity {
    WASHING, CUTTING, DRYER_CLEANING, IRRIGATION, OTHER;

    /** The activity of a value, case-insensitive; {@code null} for a blank input, 400 for an unknown activity. */
    public static ConsumptionActivity of(String activity) {
        if (activity == null || activity.isBlank()) {
            return null;
        }
        String normalized = activity.trim().toUpperCase(Locale.ROOT);
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid activity: " + Arrays.stream(values())
                    .map(Enum::name).collect(Collectors.joining(", ")) + " expected.");
        }
    }

    /** The constant name of {@link #of(String)}, {@code null} for a blank input. */
    public static String normalize(String activity) {
        ConsumptionActivity value = of(activity);
        return value == null ? null : value.name();
    }
}
