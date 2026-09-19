package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.Zone;
import org.springframework.stereotype.Service;

/**
 * Estime le besoin hydrique theorique d'une zone culturale, reference de l'economie d'eau.
 *
 * <p>Reference agronomique (FAO-56) :</p>
 * <pre>
 *   besoin (L) = surface (m2) x ET0 (mm) x Kc / efficacite du systeme
 * </pre>
 * <p>1 mm sur 1 m2 represente 1 litre, la conversion est donc directe. L'efficacite du systeme
 * represente la part de l'eau réellement utilisée par la culture (le reste s'evapore ou ruisselle) :
 * c'est precisement la marge d'economie que le module doit rendre visible.</p>
 */
@Service
public class WaterNeedService {

    /** Coefficient cultural applique quand la zone n'en definit pas. */
    public static final double DEFAULT_CROP_COEFFICIENT = 1.0;

    /** Efficacite appliquee quand le mode d'irrigation de la zone n'est pas reconnu. */
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
     * Efficacite d'application du systeme d'irrigation (0-1).
     * Le goutte-a-goutte limite fortement l'evaporation, l'aspersion et la gravite beaucoup moins.
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

    /** Besoin theorique en litres pour une zone sur un jour d'evapotranspiration donnee. */
    public double needLiters(Zone zone, double et0Mm) {
        if (zone == null || zone.getAreaHectares() == null || et0Mm <= 0) {
            return 0d;
        }
        double areaSquareMeters = zone.getAreaHectares() * SQUARE_METERS_PER_HECTARE;
        double efficiency = systemEfficiency(zone);
        return (areaSquareMeters * et0Mm * cropCoefficient(zone)) / efficiency;
    }

    /** Besoin theorique cumule sur une periode : somme des besoins journaliers. */
    public double needLiters(Zone zone, Iterable<Double> dailyEt0Mm) {
        double total = 0d;
        for (Double et0 : dailyEt0Mm) {
            total += needLiters(zone, et0 == null ? 0d : et0);
        }
        return total;
    }
}