package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import com.infineonbit.sustainablefarm.modules.watersupply.service.WeatherCodeMapper;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.ExternalServiceException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Meteo courante et previsions, exposees a l'interface.
 *
 * <p>Les coordonnees par defaut viennent de la configuration ({@code app.weather.latitude}
 * / {@code app.weather.longitude}) : une seule source de verite avec {@link AgroWeatherService},
 * qui evite qu'un client oublie les parametres et interroge un autre lieu que le site.</p>
 */
@RestController
@RequestMapping("/api/weather")
public class WeatherController {
    private final RestClient restClient;
    private final double defaultLatitude;
    private final double defaultLongitude;

    public WeatherController(
            RestClient restClient,
            @Value("${app.weather.latitude:10.63}") double defaultLatitude,
            @Value("${app.weather.longitude:-4.77}") double defaultLongitude) {
        this.restClient = restClient;
        this.defaultLatitude = defaultLatitude;
        this.defaultLongitude = defaultLongitude;
    }

    @GetMapping("/current")
    public Map<String, Object> current(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {
        Map<?, ?> data = fetch("/v1/forecast?latitude={lat}&longitude={lon}&current=temperature_2m,relative_humidity_2m,wind_speed_10m,wind_direction_10m,surface_pressure,cloud_cover,weather_code&timezone=UTC", resolveLatitude(latitude), resolveLongitude(longitude));

        Map<?, ?> current = mapValue(data, "current");
        return Map.of(
                "temperature", value(current, "temperature_2m"),
                "humidity", value(current, "relative_humidity_2m"),
                "wind_speed", value(current, "wind_speed_10m"),
                "wind_direction", value(current, "wind_direction_10m"),
                "pressure", value(current, "surface_pressure"),
                "clouds", value(current, "cloud_cover"),
                "weather_condition", WeatherCodeMapper.mapCondition(numberValue(current, "weather_code")),
                "icon", WeatherCodeMapper.mapIcon(numberValue(current, "weather_code")));
    }

    @GetMapping("/forecast")
    public List<Map<String, Object>> forecast(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {
        Map<?, ?> data = fetch("/v1/forecast?latitude={lat}&longitude={lon}&daily=temperature_2m_min,temperature_2m_max,relative_humidity_2m_mean,wind_speed_10m_mean,precipitation_probability_max,weather_code&forecast_days=7&timezone=UTC", resolveLatitude(latitude), resolveLongitude(longitude));

        Map<?, ?> daily = mapValue(data, "daily");
        List<?> dates = listValue(daily, "time");
        List<?> tMin = listValue(daily, "temperature_2m_min");
        List<?> tMax = listValue(daily, "temperature_2m_max");
        List<?> humidity = listValue(daily, "relative_humidity_2m_mean");
        List<?> wind = listValue(daily, "wind_speed_10m_mean");
        List<?> precip = listValue(daily, "precipitation_probability_max");
        List<?> codes = listValue(daily, "weather_code");
        int size = dates.size();
        if (List.of(tMin, tMax, humidity, wind, precip, codes).stream().anyMatch(values -> values.size() != size)) {
            throw new ExternalServiceException("Weather service returned inconsistent forecast data", null);
        }

        List<Map<String, Object>> forecast = new ArrayList<>();
        for (int i = 0; i < dates.size(); i++) {
            Number code = (Number) codes.get(i);
            forecast.add(Map.of(
                    "date", LocalDate.parse((String) dates.get(i)).atStartOfDay().toInstant(ZoneOffset.UTC).toString(),
                    "temperature_min", tMin.get(i),
                    "temperature_max", tMax.get(i),
                    "humidity", humidity.get(i),
                    "wind_speed", wind.get(i),
                    "rain_probability", precip.get(i),
                    "weather_condition", WeatherCodeMapper.mapCondition(code),
                    "icon", WeatherCodeMapper.mapIcon(code)));
        }
        return forecast;
    }

    /** Latitude du site quand l'appelant n'en fournit pas (configuration). */
    private double resolveLatitude(Double latitude) {
        return latitude == null ? defaultLatitude : latitude;
    }

    /** Longitude du site quand l'appelant n'en fournit pas (configuration). */
    private double resolveLongitude(Double longitude) {
        return longitude == null ? defaultLongitude : longitude;
    }

        private Map<?, ?> fetch(String uri, double latitude, double longitude) {
                try {
                        Map<?, ?> data = restClient.get().uri(uri, latitude, longitude).retrieve().body(Map.class);
                        if (data == null) {
                                throw new ExternalServiceException("Weather service returned an empty response", null);
                        }
                        return data;
                } catch (RestClientException ex) {
                        throw new ExternalServiceException("Weather service is unavailable", ex);
                }
        }

        private Map<?, ?> mapValue(Map<?, ?> source, String key) {
                Object value = source.get(key);
                if (!(value instanceof Map<?, ?> map)) {
                        throw new ExternalServiceException("Weather service response is missing " + key, null);
                }
                return map;
        }

        private List<?> listValue(Map<?, ?> source, String key) {
                Object value = source.get(key);
                if (!(value instanceof List<?> list)) {
                        throw new ExternalServiceException("Weather service response is missing " + key, null);
                }
                return list;
        }

        private Object value(Map<?, ?> source, String key) {
                Object value = source.get(key);
                if (value == null) {
                        throw new ExternalServiceException("Weather service response is missing " + key, null);
                }
                return value;
        }

        private Number numberValue(Map<?, ?> source, String key) {
                Object value = value(source, key);
                if (!(value instanceof Number number)) {
                        throw new ExternalServiceException("Weather service returned an invalid " + key, null);
                }
                return number;
        }
}
