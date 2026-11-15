package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterSource;

/**
 * Pure rules of the rainwater harvesting reservoir (specification module 5.3), without side
 * effects: usable by the alert service ({@link RainwaterTankMonitor}) as well as by the level
 * reading ({@code GET /api/water/sources/{id}/level}).
 */
public final class RainwaterTankRules {

    /** Value of {@code water_sources.type} designating a rainwater harvesting source. */
    public static final String RAINWATER_SOURCE_TYPE = "rain";

    /** Level from which an overflow is likely. */
    public static final double OVERFLOW_RISK_PERCENT = 95d;

    /** Level from which rainwater must be used first. */
    public static final double USE_RAINWATER_FIRST_PERCENT = 50d;

    /** Level below which a reservoir is considered critical (specification 2.2). */
    public static final double CRITICAL_LEVEL_PERCENT = 20d;

    /** Level above which a reservoir is considered full. */
    public static final double FULL_LEVEL_PERCENT = 90d;

    private RainwaterTankRules() {
    }

    /** True when the source is a reservoir fed by rainwater harvesting. */
    public static boolean isRainwaterTank(WaterSource source) {
        return source != null && source.getType() != null
                && RAINWATER_SOURCE_TYPE.equalsIgnoreCase(source.getType().trim());
    }

    /** Tank level as a percentage of its capacity, or null when the capacity is unknown. */
    public static Double levelPercent(WaterSource source) {
        if (source == null || source.getCapacityLiters() == null || source.getCapacityLiters() <= 0
                || source.getCurrentLevelLiters() == null) {
            return null;
        }
        return source.getCurrentLevelLiters() / source.getCapacityLiters() * 100d;
    }

    /**
     * Human-readable qualification of a level: {@code unknown}, {@code critical}, {@code moderate},
     * {@code comfortable} or {@code full}.
     */
    public static String classify(Double levelPercent) {
        if (levelPercent == null) {
            return "unknown";
        }
        if (levelPercent < CRITICAL_LEVEL_PERCENT) {
            return "critical";
        }
        if (levelPercent < USE_RAINWATER_FIRST_PERCENT) {
            return "moderate";
        }
        if (levelPercent < FULL_LEVEL_PERCENT) {
            return "comfortable";
        }
        return "full";
    }
}
