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
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldZoneRepository;
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
 * Automatic control by soil moisture (module 1.4): the rule must only create a schedule
 * when the soil is actually dry, no irrigation is running and the last
 * irrigation is old enough — and must explain every excluded zone.
 */
@ExtendWith(MockitoExtension.class)
class IrrigationAutomationServiceTest {

    @Mock
    private FieldZoneRepository fieldZoneRepository;

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
        // The repository returns null when no measurement exists for the zone (no sensor).
        when(soilMoistureRepository.findLatestMoisturePercentBefore(eq(zone.getId()), any())).thenReturn(null);

        Map<String, Object> report = service().trigger();

        assertEquals("no soil moisture reading", firstSkipReason(report));
    }

    @Test
    void seasonalThresholdFollowsTheSpecification() {
        // Rainy season, transition, cool dry season, hot dry season.
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
        when(fieldZoneRepository.findAll()).thenReturn(List.of(zone));
        return zone;
    }

    /** Checks that no schedule was created and returns the reason of the first excluded zone. */
    private String firstSkipReason(Map<String, Object> report) {
        List<?> skipped = (List<?>) report.get("skipped");
        assertTrue(skipped.size() >= 1);
        assertEquals(0, report.get("created_count"));
        return String.valueOf(((Map<?, ?>) skipped.get(0)).get("reason"));
    }

    private IrrigationAutomationService service() {
        return new IrrigationAutomationService(fieldZoneRepository, soilMoistureRepository, scheduleRepository,
                logRepository, waterNeedService, agroWeatherService, alertService);
    }
}
