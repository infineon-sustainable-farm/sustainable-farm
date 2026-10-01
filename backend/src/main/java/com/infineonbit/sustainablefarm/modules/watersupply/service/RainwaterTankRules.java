package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterSource;

/**
 * Regles pures du reservoir de collecte de pluie (module 5.3 de la specification), sans effet
 * de bord : elles sont utilisables par le service d'alerte ({@link RainwaterTankMonitor}) comme
 * par la lecture du niveau ({@code GET /api/water/sources/{id}/level}).
 */
public final class RainwaterTankRules {

    /** Valeur de {@code water_sources.type} designant une source de collecte de pluie. */
    public static final String RAINWATER_SOURCE_TYPE = "rain";

    /** Niveau a partir duquel un debordement est probable. */
    public static final double OVERFLOW_RISK_PERCENT = 95d;

    /** Niveau a partir duquel l'eau de pluie doit etre utilisee en priorite. */
    public static final double USE_RAINWATER_FIRST_PERCENT = 50d;

    /** Niveau sous lequel un reservoir est considere comme critique (specification 2.2). */
    public static final double CRITICAL_LEVEL_PERCENT = 20d;

    /** Niveau au dela duquel un reservoir est considere comme plein. */
    public static final double FULL_LEVEL_PERCENT = 90d;

    private RainwaterTankRules() {
    }

    /** Vrai si la source est un reservoir alimente par la collecte de pluie. */
    public static boolean isRainwaterTank(WaterSource source) {
        return source != null && source.getType() != null
                && RAINWATER_SOURCE_TYPE.equalsIgnoreCase(source.getType().trim());
    }

    /** Niveau du reservoir en pourcentage de sa capacite, ou null si la capacite est inconnue. */
    public static Double levelPercent(WaterSource source) {
        if (source == null || source.getCapacityLiters() == null || source.getCapacityLiters() <= 0
                || source.getCurrentLevelLiters() == null) {
            return null;
        }
        return source.getCurrentLevelLiters() / source.getCapacityLiters() * 100d;
    }

    /**
     * Qualification lisible d'un niveau : {@code unknown}, {@code critical}, {@code moderate},
     * {@code comfortable} ou {@code full}.
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
