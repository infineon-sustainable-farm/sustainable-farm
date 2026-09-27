package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterConsumption;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterSource;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Zone;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterConsumptionRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSourceRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldZoneRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Regles du module IA : niveau de reserve pour les recommandations, risque de secheresse
 * croisant reserve et consommation observee, et detection d'anomalies de consommation.
 */
@ExtendWith(MockitoExtension.class)
class AIServiceTest {

    @Mock
    private WaterSourceRepository waterSourceRepository;

    @Mock
    private FieldZoneRepository fieldZoneRepository;

    @Mock
    private WaterConsumptionRepository waterConsumptionRepository;

    @Mock
    private AlertService alertService;

    @Test
    void recommendationsFollowTheReserveLevel() {
        stubReserve(1000d, 100d);
        when(fieldZoneRepository.findAll()).thenReturn(List.of());

        List<Map<String, Object>> recommendations = service().recommendations();

        assertEquals("critical", recommendations.get(0).get("priority"));
        assertEquals("reservoir_level", recommendations.get(0).get("recommendation_type"));
        assertEquals("user_action", recommendations.get(recommendations.size() - 1).get("recommendation_type"));
    }

    @Test
    void recommendationsAreHighAtThirtyFivePercentOrLess() {
        assertEquals("high", reservePriority(1000d, 300d));
    }

    @Test
    void recommendationsAreMediumBelowFiftyFivePercent() {
        assertEquals("medium", reservePriority(1000d, 400d));
    }

    @Test
    void recommendationsAreLowWithAComfortableReserve() {
        assertEquals("low", reservePriority(1000d, 800d));
    }

    @Test
    void recommendationsMentionDripEquippedZones() {
        stubReserve(1000d, 800d);
        when(fieldZoneRepository.findAll()).thenReturn(List.of(zone("drip"), zone("goutte-a-goutte"), zone("sprinkler")));

        List<Map<String, Object>> recommendations = service().recommendations();

        assertTrue(recommendations.stream().anyMatch(rec -> "drip_efficiency".equals(rec.get("recommendation_type"))
                && String.valueOf(rec.get("recommendation")).startsWith("2 zone")));
    }

    @Test
    void droughtIsCriticalWhenTheReserveCoversOneDayOrLess() {
        stubReserve(1000d, 500d);
        // 7 jours de consommation a 500 L/jour : la reserve ne couvre qu'une journee.
        when(waterConsumptionRepository.findAll()).thenReturn(dailyConsumptions(7, 500d));

        Map<String, Object> prediction = service().droughtPrediction();

        assertEquals("CRITICAL", prediction.get("risk_level"));
        assertEquals(1.0d, prediction.get("days_of_reserve_remaining"));
    }

    @Test
    void droughtRisesToHighBelowThirtyPercentOrThreeDays() {
        stubReserve(1000d, 250d);
        when(waterConsumptionRepository.findAll()).thenReturn(List.of());

        Map<String, Object> prediction = service().droughtPrediction();

        assertEquals("HIGH", prediction.get("risk_level"));
        assertEquals(25L, prediction.get("reservoir_level_percentage"));
        assertEquals(null, prediction.get("days_of_reserve_remaining"));
    }

    @Test
    void droughtIsLowWithComfortableReserveAndNoRecentConsumption() {
        stubReserve(1000d, 800d);
        when(waterConsumptionRepository.findAll()).thenReturn(List.of());

        Map<String, Object> prediction = service().droughtPrediction();

        assertEquals("LOW", prediction.get("risk_level"));
        assertEquals(0.85d, prediction.get("confidence"));
        assertEquals(7, prediction.get("consumption_window_days"));
    }

    @Test
    void analyzeFlagsConsumptionAboveFiftyPercentOfTheAverage() {
        when(waterConsumptionRepository.findAll()).thenReturn(List.of(
                consumption(100d, 1), consumption(100d, 2), consumption(400d, 3)));

        Map<String, Object> result = service().analyze();

        assertEquals(1, result.get("anomalies_count"));
        assertEquals(3, result.get("analyzed_records"));
        List<?> anomalies = (List<?>) result.get("anomalies");
        assertEquals("possible_leak", ((Map<?, ?>) anomalies.get(0)).get("type"));
        verify(alertService).raise(eq("critical"), anyString(), anyString(), anyString());
    }

    @Test
    void analyzeStaysSilentWhenConsumptionIsRegular() {
        when(waterConsumptionRepository.findAll()).thenReturn(List.of(
                consumption(100d, 1), consumption(110d, 2), consumption(90d, 3)));

        Map<String, Object> result = service().analyze();

        assertEquals(0, result.get("anomalies_count"));
        verify(alertService, never()).raise(anyString(), anyString(), anyString(), anyString());
    }

    /** Priorite de la premiere recommandation pour une reserve donnee (capacite, niveau). */
    private String reservePriority(double capacity, double level) {
        stubReserve(capacity, level);
        when(fieldZoneRepository.findAll()).thenReturn(List.of());
        return String.valueOf(service().recommendations().get(0).get("priority"));
    }

    private void stubReserve(double capacity, double level) {
        WaterSource source = new WaterSource();
        source.setCapacityLiters(capacity);
        source.setCurrentLevelLiters(level);
        when(waterSourceRepository.findAll()).thenReturn(List.of(source));
    }

    private Zone zone(String irrigationMethod) {
        Zone zone = new Zone();
        zone.setIrrigationMethod(irrigationMethod);
        return zone;
    }

    /** Une mesure de consommation par jour, sur la fenetre d'analyse de 7 jours (J-0 a J-6). */
    private List<WaterConsumption> dailyConsumptions(int days, double litersPerDay) {
        return java.util.stream.IntStream.range(0, days)
                .mapToObj(day -> consumption(litersPerDay, day))
                .toList();
    }

    private WaterConsumption consumption(double liters, int daysAgo) {
        WaterConsumption consumption = new WaterConsumption();
        consumption.setConsumptionLiters(liters);
        consumption.setConsumptionDate(Instant.now().minus(daysAgo, ChronoUnit.DAYS));
        return consumption;
    }

    private AIService service() {
        return new AIService(waterSourceRepository, fieldZoneRepository, waterConsumptionRepository, alertService);
    }
}
