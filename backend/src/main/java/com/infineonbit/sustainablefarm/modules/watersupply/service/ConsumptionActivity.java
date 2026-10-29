package com.infineonbit.sustainablefarm.modules.watersupply.service;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Activities a water consumption can be attributed to (Product Transformation integration).
 *
 * <p>Stored as plain text in {@code water_consumption.activity}: the list must stay aligned with
 * the activities Product Transformation actually runs (to confirm with that module).</p>
 */
public enum ConsumptionActivity {
    WASHING, CUTTING, DRYER_CLEANING, IRRIGATION, OTHER;

    /** Normalized value (upper case), {@code null} for a blank input, 400 for an unknown activity. */
    public static String normalize(String activity) {
        if (activity == null || activity.isBlank()) {
            return null;
        }
        String normalized = activity.trim().toUpperCase(Locale.ROOT);
        try {
            return valueOf(normalized).name();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid activity: " + Arrays.stream(values())
                    .map(Enum::name).collect(Collectors.joining(", ")) + " expected.");
        }
    }
}
