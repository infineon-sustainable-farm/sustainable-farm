package com.infineonbit.sustainablefarm.modules.watersupply.service;

import java.util.Map;

/**
 * Maps WMO weather codes to human-readable conditions and icons.
 * See https://open-meteo.com/en/docs for the WMO code reference.
 */
public final class WeatherCodeMapper {

    private WeatherCodeMapper() {
    }

    public static String mapCondition(Number code) {
        return CONDITIONS.getOrDefault(normalize(code), "Inconnu");
    }

    public static String mapIcon(Number code) {
        return ICONS.getOrDefault(normalize(code), "question");
    }

    private static int normalize(Number code) {
        if (code == null) {
            return -1;
        }
        int wmo = code.intValue();
        if (wmo >= 45 && wmo <= 48) return 45;
        if (wmo >= 51 && wmo <= 57) return 51;
        if (wmo >= 61 && wmo <= 67) return 61;
        if (wmo >= 71 && wmo <= 77) return 71;
        if (wmo >= 80 && wmo <= 82) return 80;
        if (wmo >= 95) return 95;
        return wmo;
    }

    private static final Map<Integer, String> CONDITIONS = Map.ofEntries(
            Map.entry(-1, "Inconnu"),
            Map.entry(0, "Ensoleille"),
            Map.entry(1, "Tres ensoleille"),
            Map.entry(2, "Partiellement nuageux"),
            Map.entry(3, "Nuageux"),
            Map.entry(45, "Brouillard"),
            Map.entry(51, "Bruine"),
            Map.entry(61, "Pluie"),
            Map.entry(71, "Neige"),
            Map.entry(80, "Averses"),
            Map.entry(95, "Orage"));

    private static final Map<Integer, String> ICONS = Map.ofEntries(
            Map.entry(-1, "question"),
            Map.entry(0, "sun"),
            Map.entry(1, "sun"),
            Map.entry(2, "cloud-sun"),
            Map.entry(3, "cloud"),
            Map.entry(45, "fog"),
            Map.entry(51, "drizzle"),
            Map.entry(61, "rain"),
            Map.entry(71, "snow"),
            Map.entry(80, "rain"),
            Map.entry(95, "storm"));
}
