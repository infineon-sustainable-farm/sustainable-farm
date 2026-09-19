package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import com.infineonbit.sustainablefarm.modules.watersupply.service.WeatherCodeMapper;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.ExternalServiceException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {
    private final RestClient restClient;

    public WeatherController(RestClient restClient) {
        this.restClient = restClient;
    }

    @GetMapping("/current")
    public Map<String, Object> current(
            @RequestParam(defaultValue = "10.5") double latitude,
            @RequestParam(defaultValue = "-61.2") double longitude) {
        Map<?, ?> data = fetch("/v1/forecast?latitude={lat}&longitude={lon}&current=temperature_2m,relative_humidity_2m,wind_speed_10m,wind_direction_10m,surface_pressure,cloud_cover,weather_code&timezone=UTC", latitude, longitude);

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
            @RequestParam(defaultValue = "10.5") double latitude,
            @RequestParam(defaultValue = "-61.2") double longitude) {
        Map<?, ?> data = fetch("/v1/forecast?latitude={lat}&longitude={lon}&daily=temperature_2m_min,temperature_2m_max,relative_humidity_2m_mean,wind_speed_10m_mean,precipitation_probability_max,weather_code&forecast_days=7&timezone=UTC", latitude, longitude);

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
