package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationLog;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationSchedule;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Zone;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IrrigationLogRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IrrigationScheduleRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.SoilMoistureReadingRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.ZoneRepository;
import java.time.Instant;
import java.time.Month;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pilotage automatique par l'humidite du sol (module 1.4) : la regle ne doit creer un planning
 * que lorsque le sol est reellement sec, qu'aucun arrosage n'est en cours et que le dernier
 * arrosage est assez ancien — et expliquer chaque zone ecartee.
 */
@ExtendWith(MockitoExtension.class)
class IrrigationAutomationServiceTest {

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private SoilMoistureReadingRepository soilMoistureRepository;

    @Mock
    private IrrigationScheduleRepository scheduleRepository;

    @Mock
    private IrrigationLogRepository logRepository;

    @Mock
    private WaterNeedService waterNeedService;

    @Mock
    private AgroWeatherService agroWeatherService;

    @Mock
    private AlertService alertService;

    @Test
    void createsAnAutomaticScheduleWhenSoilIsDry() {
        Zone zone = stubSingleZone();
        when(agroWeatherService.et0Between(any(), any())).thenReturn(5d);
        when(soilMoistureRepository.findLatestMoisturePercentBefore(eq(zone.getId()), any())).thenReturn(30d);
        when(scheduleRepository.findByZoneId(zone.getId())).thenReturn(List.of());
        when(logRepository.findForZoneBetween(eq(zone.getId()), any(), any())).thenReturn(List.of());
        when(waterNeedService.needLiters(zone, 5d)).thenReturn(1234.5d);
        when(scheduleRepository.save(any(IrrigationSchedule.class))).thenAnswer(call -> call.getArgument(0));

        Map<String, Object> report = service().trigger();

        assertEquals(1, report.get("created_count"));
        assertEquals(0, report.get("skipped_count"));
        assertEquals(5d, report.get("et0_mm"));

        ArgumentCaptor<IrrigationSchedule> saved = ArgumentCaptor.forClass(IrrigationSchedule.class);
        verify(scheduleRepository).save(saved.capture());
        assertEquals(IrrigationAutomationService.TRIGGER_SOURCE_AUTO, saved.getValue().getTriggerSource());
        assertEquals("scheduled", saved.getValue().getStatus());
        assertEquals(1235d, saved.getValue().getWaterQuantityLiters());
        assertEquals(zone.getId(), saved.getValue().getZoneId());
    }

    @Test
    void skipsZonesAboveTheSeasonalThreshold() {
        Zone zone = stubSingleZone();
        when(agroWeatherService.et0Between(any(), any())).thenReturn(5d);
        when(soilMoistureRepository.findLatestMoisturePercentBefore(eq(zone.getId()), any())).thenReturn(80d);

        Map<String, Object> report = service().trigger();

        assertEquals("soil moisture above the seasonal threshold", firstSkipReason(report));
    }

    @Test
    void skipsZonesWithAnOpenSchedule() {
        Zone zone = stubSingleZone();
        when(agroWeatherService.et0Between(any(), any())).thenReturn(5d);
        when(soilMoistureRepository.findLatestMoisturePercentBefore(eq(zone.getId()), any())).thenReturn(30d);
        IrrigationSchedule open = new IrrigationSchedule();
        open.setStatus("scheduled");
        when(scheduleRepository.findByZoneId(zone.getId())).thenReturn(List.of(open));

        Map<String, Object> report = service().trigger();

        assertEquals("an irrigation is already planned or running", firstSkipReason(report));
    }

    @Test
    void skipsZonesIrrigatedLessThanTwentyFourHoursAgo() {
        Zone zone = stubSingleZone();
        when(agroWeatherService.et0Between(any(), any())).thenReturn(5d);
        when(soilMoistureRepository.findLatestMoisturePercentBefore(eq(zone.getId()), any())).thenReturn(30d);
        when(scheduleRepository.findByZoneId(zone.getId())).thenReturn(List.of());
        IrrigationLog recent = new IrrigationLog();
        recent.setActualStartTime(Instant.now());
        when(logRepository.findForZoneBetween(eq(zone.getId()), any(), any())).thenReturn(List.of(recent));

        Map<String, Object> report = service().trigger();

        assertEquals("irrigated less than 24 hours ago", firstSkipReason(report));
    }

    @Test
    void reportsZonesWithoutSoilSensor() {
        Zone zone = stubSingleZone();
        when(agroWeatherService.et0Between(any(), any())).thenReturn(5d);
        // Le repository renvoie null quand aucune mesure n'existe pour la zone (pas de capteur).
        when(soilMoistureRepository.findLatestMoisturePercentBefore(eq(zone.getId()), any())).thenReturn(null);

        Map<String, Object> report = service().trigger();

        assertEquals("no soil moisture reading", firstSkipReason(report));
    }

    @Test
    void seasonalThresholdFollowsTheSpecification() {
        // Saison des pluies, transition, saison seche fraiche, saison seche chaude.
        assertEquals(40d, IrrigationAutomationService.seasonalThresholdPercent(Month.AUGUST));
        assertEquals(45d, IrrigationAutomationService.seasonalThresholdPercent(Month.MAY));
        assertEquals(50d, IrrigationAutomationService.seasonalThresholdPercent(Month.DECEMBER));
        assertEquals(55d, IrrigationAutomationService.seasonalThresholdPercent(Month.MARCH));
        assertEquals("rainy", IrrigationAutomationService.seasonLabel(Month.AUGUST));
        assertEquals("hot dry", IrrigationAutomationService.seasonLabel(Month.FEBRUARY));
    }

    private Zone stubSingleZone() {
        Zone zone = new Zone();
        zone.setName("Zone A");
        zone.setAreaHectares(1.0);
        when(zoneRepository.findAll()).thenReturn(List.of(zone));
        return zone;
    }

    /** Verifie qu'aucun planning n'a ete cree et renvoie le motif de la premiere zone ecartee. */
    private String firstSkipReason(Map<String, Object> report) {
        List<?> skipped = (List<?>) report.get("skipped");
        assertTrue(skipped.size() >= 1);
        assertEquals(0, report.get("created_count"));
        return String.valueOf(((Map<?, ?>) skipped.get(0)).get("reason"));
    }

    private IrrigationAutomationService service() {
        return new IrrigationAutomationService(zoneRepository, soilMoistureRepository, scheduleRepository,
                logRepository, waterNeedService, agroWeatherService, alertService);
    }
}
