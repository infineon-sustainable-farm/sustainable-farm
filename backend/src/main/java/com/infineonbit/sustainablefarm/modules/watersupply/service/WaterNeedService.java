package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.Zone;
import org.springframework.stereotype.Service;

/**
 * Estimates the theoretical water need of a crop zone, the reference for water savings.
 *
 * <p>Agronomic reference (FAO-56):</p>
 * <pre>
 *   need (L) = area (m2) x ET0 (mm) x Kc / system efficiency
 * </pre>
 * <p>1 mm over 1 m2 represents 1 litre, so the conversion is direct. The system efficiency
 * represents the share of water actually used by the crop (the rest evaporates or runs off):
 * this is precisely the savings margin the module must make visible.</p>
 */
@Service
public class WaterNeedService {

    /** Crop coefficient applied when the zone does not define one. */
    public static final double DEFAULT_CROP_COEFFICIENT = 1.0;

    /** Efficiency applied when the zone's irrigation method is not recognized. */
    public static final double DEFAULT_SYSTEM_EFFICIENCY = 0.80;

    /** 1 hectare = 10 000 m2. */
    private static final double SQUARE_METERS_PER_HECTARE = 10_000d;

    public double cropCoefficient(Zone zone) {
        if (zone == null || zone.getCropCoefficient() == null || zone.getCropCoefficient() <= 0) {
            return DEFAULT_CROP_COEFFICIENT;
        }
        return zone.getCropCoefficient();
    }

    /**
     * Application efficiency of the irrigation system (0-1).
     * Drip strongly limits evaporation, sprinkler and gravity much less so.
     */
    public double systemEfficiency(Zone zone) {
        String method = zone == null || zone.getIrrigationMethod() == null
                ? "" : zone.getIrrigationMethod().trim().toLowerCase();
        return switch (method) {
            case "drip", "goutte-a-goutte", "goutte", "goutte a goutte" -> 0.90;
            case "sprinkler", "aspersion" -> 0.75;
            case "gravity", "gravitaire", "flood", "submersion" -> 0.60;
            default -> DEFAULT_SYSTEM_EFFICIENCY;
        };
    }

    /** Readable label of the irrigation method (used in the UI and messages). */
    public String methodLabel(Zone zone) {
        String method = zone == null || zone.getIrrigationMethod() == null
                ? "" : zone.getIrrigationMethod().trim().toLowerCase();
        return switch (method) {
            case "drip", "goutte-a-goutte", "goutte", "goutte a goutte" -> "drip";
            case "sprinkler", "aspersion" -> "sprinkler";
            case "gravity", "gravitaire", "flood", "submersion" -> "gravity";
            default -> "not specified";
        };
    }

    /** Theoretical need in litres for a zone on a given evapotranspiration day. */
    public double needLiters(Zone zone, double et0Mm) {
        if (zone == null || zone.getAreaHectares() == null || et0Mm <= 0) {
            return 0d;
        }
        double areaSquareMeters = zone.getAreaHectares() * SQUARE_METERS_PER_HECTARE;
        double efficiency = systemEfficiency(zone);
        return (areaSquareMeters * et0Mm * cropCoefficient(zone)) / efficiency;
    }

    /** Theoretical need accumulated over a period: sum of the daily needs. */
    public double needLiters(Zone zone, Iterable<Double> dailyEt0Mm) {
        double total = 0d;
        for (Double et0 : dailyEt0Mm) {
            total += needLiters(zone, et0 == null ? 0d : et0);
        }
        return total;
    }
}