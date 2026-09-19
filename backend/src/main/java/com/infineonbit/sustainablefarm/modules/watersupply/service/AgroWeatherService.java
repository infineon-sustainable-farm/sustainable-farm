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
 * Données agro-météorologiques nécessaires au pilotage de l'économie d'eau : évapotranspiration
 * de référence (ET0) pour estimer le besoin des cultures, et pluie prévue (quantité + probabilité)
 * pour décider de reporter une irrigation.
 *
 * <p>Les jours passés sont demandés en plus des jours à venir ({@code past_days}) : c'est ce qui
 * permet de comparer la consommation réelle au besoin théorique sur la période écoulée.</p>
 *
 * <p>Les réponses sont mises en cache {@value #CACHE_DURATION_MINUTES} minutes afin qu'un affichage
 * de dashboard ne déclenche pas un appel externe par requête.</p>
 */
@Service
public class AgroWeatherService {

    /** Durée de validité du cache local des données agro-météo. */
    public static final int CACHE_DURATION_MINUTES = 30;

    /** Valeur d'ET0 utilisée quand la météo n'est pas joignable (climat tropical humide). */
    public static final double FALLBACK_ET0_MM = 4.0;

    /** Nombre de jours passés récupérés (comparaison réel / besoin théorique). */
    private static final int PAST_DAYS = 7;

    /** Nombre de jours à venir récupérés (suggestions de report). */
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

    /** Une journée agro-météorologique. */
    public record DailyAgro(LocalDate date, Double et0Mm, Double rainMm, Double rainProbability) {
    }

    /** Saison agro-météorologique complète (jours passés + jours à venir). */
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

    /** Données agro-météo d'une date précise (vide si hors de la fenêtre récupérée). */
    public Optional<DailyAgro> forDate(LocalDate date) {
        return daily().stream().filter(day -> date.equals(day.date())).findFirst();
    }

    /**
     * ET0 moyenne des {@code days} derniers jours (aujourd'hui inclus).
     * Retourne la moyenne des valeurs disponibles, ou une valeur par défaut si l'API est
     * momentanément indisponible : l'estimation du besoin ne doit pas casser le dashboard.
     */
    public double averageEt0PastDays(int days) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate from = today.minusDays(Math.max(1, days) - 1L);
        return et0Between(from, today);
    }

    /** ET0 moyenne sur un intervalle de dates (valeur par défaut si aucune donnée disponible). */
    public double et0Between(LocalDate from, LocalDate to) {
        return daily().stream()
                .filter(day -> !day.date().isBefore(from) && !day.date().isAfter(to))
                .map(DailyAgro::et0Mm)
                .filter(value -> value != null && value > 0)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(FALLBACK_ET0_MM);
    }

    /** ET0 de chaque jour récupéré, dans l'ordre chronologique (série d'économie d'eau). */
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
