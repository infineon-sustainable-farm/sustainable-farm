package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import com.infineonbit.sustainablefarm.modules.watersupply.service.WeatherCodeMapper;
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
        Map<?, ?> data = restClient.get()
                .uri("/v1/forecast?latitude={lat}&longitude={lon}&current=temperature_2m,relative_humidity_2m,wind_speed_10m,wind_direction_10m,surface_pressure,cloud_cover,weather_code&timezone=UTC",
                        latitude, longitude)
                .retrieve()
                .body(Map.class);

        Map<?, ?> current = (Map<?, ?>) data.get("current");
        return Map.of(
                "temperature", current.get("temperature_2m"),
                "humidity", current.get("relative_humidity_2m"),
                "wind_speed", current.get("wind_speed_10m"),
                "wind_direction", current.get("wind_direction_10m"),
                "pressure", current.get("surface_pressure"),
                "clouds", current.get("cloud_cover"),
                "weather_condition", WeatherCodeMapper.mapCondition((Number) current.get("weather_code")),
                "icon", WeatherCodeMapper.mapIcon((Number) current.get("weather_code")));
    }

    @GetMapping("/forecast")
    public List<Map<String, Object>> forecast(
            @RequestParam(defaultValue = "10.5") double latitude,
            @RequestParam(defaultValue = "-61.2") double longitude) {
        Map<?, ?> data = restClient.get()
                .uri("/v1/forecast?latitude={lat}&longitude={lon}&daily=temperature_2m_min,temperature_2m_max,relative_humidity_2m_mean,wind_speed_10m_mean,precipitation_probability_max,weather_code&forecast_days=7&timezone=UTC",
                        latitude, longitude)
                .retrieve()
                .body(Map.class);

        Map<?, ?> daily = (Map<?, ?>) data.get("daily");
        List<?> dates = (List<?>) daily.get("time");
        List<?> tMin = (List<?>) daily.get("temperature_2m_min");
        List<?> tMax = (List<?>) daily.get("temperature_2m_max");
        List<?> humidity = (List<?>) daily.get("relative_humidity_2m_mean");
        List<?> wind = (List<?>) daily.get("wind_speed_10m_mean");
        List<?> precip = (List<?>) daily.get("precipitation_probability_max");
        List<?> codes = (List<?>) daily.get("weather_code");

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
}
