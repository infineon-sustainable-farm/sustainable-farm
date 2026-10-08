package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

import com.infineonbit.sustainablefarm.modules.watersupply.exception.ExternalServiceException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Agro-weather data: reading the Open-Meteo response, ET0 average with fallback value,
 * ET0 series and error propagation when the external API is unavailable.
 */
class AgroWeatherServiceTest {

    private static final double FALLBACK = AgroWeatherService.FALLBACK_ET0_MM;

    @Test
    void parsesTheDailySeriesFromTheWeatherApi() {
        Map<String, Object> daily = new LinkedHashMap<>();
        daily.put("time", List.of("2026-09-01", "2026-09-02"));
        daily.put("et0_fao_evapotranspiration", List.of(4.5, 5.0));
        daily.put("precipitation_sum", List.of(0.0, 12.0));
        daily.put("precipitation_probability_max", List.of(10, 80));

        List<AgroWeatherService.DailyAgro> series = service().parse(Map.of("daily", daily));

        assertEquals(2, series.size());
        assertEquals(LocalDate.of(2026, 9, 1), series.get(0).date());
        assertEquals(4.5d, series.get(0).et0Mm());
        assertEquals(12.0d, series.get(1).rainMm());
        assertEquals(80d, series.get(1).rainProbability());
    }

    @Test
    void missingOrIncompleteDailyBlockIsReportedAsExternalError() {
        assertThrows(ExternalServiceException.class, () -> service().parse(Map.of()));
        assertThrows(ExternalServiceException.class, () -> service().parse(null));

        // A day without a value stays readable: the missing field becomes null, not an error.
        Map<String, Object> shortDaily = new LinkedHashMap<>();
        shortDaily.put("time", List.of("2026-09-01", "2026-09-02"));
        shortDaily.put("et0_fao_evapotranspiration", List.of(4.5));
        shortDaily.put("precipitation_sum", List.of(0.0));
        shortDaily.put("precipitation_probability_max", List.of(10));

        List<AgroWeatherService.DailyAgro> series = service().parse(Map.of("daily", shortDaily));

        assertEquals(2, series.size());
        assertEquals(4.5d, series.get(0).et0Mm());
        assertEquals(null, series.get(1).et0Mm());
    }

    @Test
    void averageEt0IgnoresMissingDaysAndUsesTheFallbackWhenNothingIsAvailable() {
        AgroWeatherService service = withSeries(
                day(LocalDate.of(2026, 9, 1), 4.0d),
                day(LocalDate.of(2026, 9, 2), null),
                day(LocalDate.of(2026, 9, 3), 6.0d));

        assertEquals(5.0d, service.et0Between(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3)), 0.001d);

        AgroWeatherService empty = withSeries();
        assertEquals(FALLBACK, empty.et0Between(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3)), 0.001d);
    }

    @Test
    void et0SeriesKeepsTheChronologyAndReplacesMissingValues() {
        AgroWeatherService service = withSeries(
                day(LocalDate.of(2026, 9, 1), 4.0d),
                day(LocalDate.of(2026, 9, 2), null));

        List<Double> series = service.et0Series();

        assertEquals(List.of(4.0d, FALLBACK), series);
    }

    @Test
    void forDateFindsOnlyTheDaysInsideTheWindow() {
        AgroWeatherService service = withSeries(day(LocalDate.of(2026, 9, 1), 4.0d));

        assertTrue(service.forDate(LocalDate.of(2026, 9, 1)).isPresent());
        assertFalse(service.forDate(LocalDate.of(2026, 8, 1)).isPresent());
    }

    @Test
    void anUnreachableWeatherApiIsReportedAsExternalError() {
        RestClient unavailable = mock(RestClient.class, RETURNS_DEEP_STUBS);
        when(unavailable.get()).thenThrow(new RestClientException("weather api down"));
        AgroWeatherService service = new AgroWeatherService(unavailable, 10.63d, -4.77d);

        assertThrows(ExternalServiceException.class, service::daily);
    }

    /** Service with a provided daily series: network reading is not tested here. */
    private AgroWeatherService withSeries(AgroWeatherService.DailyAgro... days) {
        AgroWeatherService service = spy(service());
        doReturn(new ArrayList<>(List.of(days))).when(service).daily();
        return service;
    }

    private AgroWeatherService.DailyAgro day(LocalDate date, Double et0) {
        return new AgroWeatherService.DailyAgro(date, et0, 0d, 0d);
    }

    private AgroWeatherService service() {
        return new AgroWeatherService(mock(RestClient.class), 10.63d, -4.77d);
    }
}
