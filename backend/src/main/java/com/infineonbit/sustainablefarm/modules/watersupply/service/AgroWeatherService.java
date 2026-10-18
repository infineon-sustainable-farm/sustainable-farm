package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.exception.ExternalServiceException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Agro-weather data needed to drive water savings: reference evapotranspiration
 * (ET0) to estimate the crops' need, and forecast rain (amount + probability)
 * to decide whether to postpone an irrigation.
 *
 * <p>Past days are requested in addition to the upcoming ones ({@code past_days}): this is
 * what allows comparing actual consumption with the theoretical need over the elapsed
 * period.</p>
 *
 * <p>Responses are cached for {@value #CACHE_DURATION_MINUTES} minutes so that rendering a
 * dashboard does not trigger an external call per request.</p>
 */
@Service
public class AgroWeatherService {

    /** Lifetime of the local agro-weather data cache. */
    public static final int CACHE_DURATION_MINUTES = 30;

    /** ET0 value used when the weather service is unreachable (humid tropical climate). */
    public static final double FALLBACK_ET0_MM = 4.0;

    /** Number of past days fetched (actual vs theoretical need comparison). */
    private static final int PAST_DAYS = 7;

    /** Number of upcoming days fetched (postponement suggestions). */
    private static final int FORECAST_DAYS = 7;

    private static final String FORECAST_URI = "/v1/forecast?latitude={lat}&longitude={lon}"
            + "&daily=et0_fao_evapotranspiration,precipitation_sum,precipitation_probability_max"
            + "&past_days=" + PAST_DAYS + "&forecast_days=" + FORECAST_DAYS + "&timezone=UTC";

    private final RestClient restClient;
    private final double latitude;
    private final double longitude;

    private volatile List<DailyAgro> cache = List.of();
    private volatile Instant cachedAt = Instant.EPOCH;

    public AgroWeatherService(
            RestClient restClient,
            @Value("${app.weather.latitude:10.5}") double latitude,
            @Value("${app.weather.longitude:-61.2}") double longitude) {
        this.restClient = restClient;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /** One agro-weather day. */
    public record DailyAgro(LocalDate date, Double et0Mm, Double rainMm, Double rainProbability) {
    }

    /** Full agro-weather window (past days + upcoming days). */
    public List<DailyAgro> daily() {
        Instant now = Instant.now();
        List<DailyAgro> current = cache;
        if (!current.isEmpty() && Duration.between(cachedAt, now).toMinutes() < CACHE_DURATION_MINUTES) {
            return current;
        }
        List<DailyAgro> fresh = fetch();
        cache = fresh;
        cachedAt = now;
        return fresh;
    }

    /** Agro-weather data of a precise date (empty when outside the fetched window). */
    public Optional<DailyAgro> forDate(LocalDate date) {
        return daily().stream().filter(day -> date.equals(day.date())).findFirst();
    }

    /**
     * Average ET0 over the last {@code days} days (today included).
     * Returns the average of the available values, or a default value when the API is
     * temporarily unavailable: the need estimation must not break the dashboard.
     */
    public double averageEt0PastDays(int days) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate from = today.minusDays(Math.max(1, days) - 1L);
        return et0Between(from, today);
    }

    /** Average ET0 over a date interval (default value when no data is available). */
    public double et0Between(LocalDate from, LocalDate to) {
        return daily().stream()
                .filter(day -> !day.date().isBefore(from) && !day.date().isAfter(to))
                .map(DailyAgro::et0Mm)
                .filter(value -> value != null && value > 0)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(FALLBACK_ET0_MM);
    }

    /** ET0 of each fetched day, in chronological order (water savings series). */
    public List<Double> et0Series() {
        List<Double> series = new ArrayList<>();
        for (DailyAgro day : daily()) {
            series.add(day.et0Mm() == null ? FALLBACK_ET0_MM : day.et0Mm());
        }
        return series;
    }

    private List<DailyAgro> fetch() {
        Map<?, ?> data;
        try {
            data = restClient.get().uri(FORECAST_URI, latitude, longitude).retrieve().body(Map.class);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Weather service is unavailable", ex);
        }
        return parse(data);
    }

    /**
     * Open-Meteo response parsing (separation of concerns: the network on one side, the
     * field reading on the other, which makes the parsing testable without calling the API).
     */
    List<DailyAgro> parse(Map<?, ?> data) {
        if (data == null || !(data.get("daily") instanceof Map<?, ?> daily)) {
            throw new ExternalServiceException("Weather service response is missing daily data", null);
        }
        List<?> dates = list(daily, "time");
        List<?> et0 = list(daily, "et0_fao_evapotranspiration");
        List<?> rain = list(daily, "precipitation_sum");
        List<?> probability = list(daily, "precipitation_probability_max");

        List<DailyAgro> result = new ArrayList<>();
        for (int i = 0; i < dates.size(); i++) {
            result.add(new DailyAgro(
                    LocalDate.parse(String.valueOf(dates.get(i))),
                    numberOrNull(et0, i),
                    numberOrNull(rain, i),
                    numberOrNull(probability, i)));
        }
        return result;
    }

    private List<?> list(Map<?, ?> source, String key) {
        Object value = source.get(key);
        if (!(value instanceof List<?> list)) {
            throw new ExternalServiceException("Weather service response is missing " + key, null);
        }
        return list;
    }

    private Double numberOrNull(List<?> values, int index) {
        if (index >= values.size()) {
            return null;
        }
        return values.get(index) instanceof Number number ? number.doubleValue() : null;
    }
}
