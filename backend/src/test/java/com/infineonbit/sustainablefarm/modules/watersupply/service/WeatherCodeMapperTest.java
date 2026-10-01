package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Correspondance des codes meteo WMO (Open-Meteo) : les plages de codes doivent rester
 * traduites comme l'attend l'interface (condition + icone), code inconnu compris.
 */
class WeatherCodeMapperTest {

    @Test
    void mapsClearAndCloudyCodes() {
        assertEquals("Sunny", WeatherCodeMapper.mapCondition(0));
        assertEquals("Very sunny", WeatherCodeMapper.mapCondition(1));
        assertEquals("Partly cloudy", WeatherCodeMapper.mapCondition(2));
        assertEquals("Cloudy", WeatherCodeMapper.mapCondition(3));
    }

    @Test
    void mapsCodeRangesToASingleCondition() {
        assertEquals("Fog", WeatherCodeMapper.mapCondition(48));      // 45-48
        assertEquals("Drizzle", WeatherCodeMapper.mapCondition(57));   // 51-57
        assertEquals("Rain", WeatherCodeMapper.mapCondition(67));      // 61-67
        assertEquals("Snow", WeatherCodeMapper.mapCondition(77));      // 71-77
        assertEquals("Showers", WeatherCodeMapper.mapCondition(82));   // 80-82
        assertEquals("Thunderstorm", WeatherCodeMapper.mapCondition(99)); // 95+
    }

    @Test
    void mapsIconsForTheInterface() {
        assertEquals("sun", WeatherCodeMapper.mapIcon(0));
        assertEquals("cloud-sun", WeatherCodeMapper.mapIcon(2));
        assertEquals("cloud", WeatherCodeMapper.mapIcon(3));
        assertEquals("fog", WeatherCodeMapper.mapIcon(45));
        assertEquals("drizzle", WeatherCodeMapper.mapIcon(53));
        assertEquals("rain", WeatherCodeMapper.mapIcon(63));
        assertEquals("snow", WeatherCodeMapper.mapIcon(75));
        assertEquals("rain", WeatherCodeMapper.mapIcon(81));
        assertEquals("storm", WeatherCodeMapper.mapIcon(96));
    }

    @Test
    void unknownOrMissingCodeIsNeverAnError() {
        assertEquals("Unknown", WeatherCodeMapper.mapCondition(7));
        assertEquals("Unknown", WeatherCodeMapper.mapCondition(null));
        assertEquals("question", WeatherCodeMapper.mapIcon(7));
        assertEquals("question", WeatherCodeMapper.mapIcon(null));
    }
}
