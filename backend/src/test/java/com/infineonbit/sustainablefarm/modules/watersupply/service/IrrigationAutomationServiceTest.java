package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.modules.watersupply.config.WaterIntegrationProperties;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Field;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationLog;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationSchedule;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Zone;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IrrigationLogRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IrrigationScheduleRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.SoilMoistureReadingRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldZoneRepository;
import java.time.Instant;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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

    @Mock
    private FieldRepository fieldRepository;

    @Mock
    private RainfallService rainfallService;

    @Mock
    private WaterUnavailabilityService unavailabilityService;

    @Mock
    private ZoneBlockMappingService zoneBlockMappingService;

    @Mock
    private PlantsGrowthPort plantsGrowthPort;

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
    void skipsZonesUnderPlannedWaterUnavailability() {
        Zone zone = stubDryZoneWithoutRecentIrrigation();
        when(unavailabilityService.isZoneUnavailable(eq(zone.getId()), any())).thenReturn(true);

        Map<String, Object> report = service().trigger();

        assertEquals("planned water unavailability in progress", firstSkipReason(report));
    }

    @Test
    void skipsZonesWhoseFarmGaugesRecordedRecentRain() {
        Zone zone = stubDryZoneWithoutRecentIrrigation();
        UUID farmId = UUID.randomUUID();
        zone.setFieldId(UUID.randomUUID());
        Field field = new Field();
        field.setFarmId(farmId);
        when(fieldRepository.findById(zone.getFieldId())).thenReturn(Optional.of(field));
        when(rainfallService.forFarms(eq(List.of(farmId)), any(), any()))
                .thenReturn(new RainfallService.RainfallSummary(6.2d, Map.of(), List.of()));

        Map<String, Object> report = service().trigger();

        // Default threshold: 5 mm over the last 24 h.
        assertEquals("recent rain 6.2 mm over the last 24 h", firstSkipReason(report));
    }

    @Test
    void appliesThePlantsGrowthPhaseFactorToTheVolume() {
        Zone zone = stubDryZoneWithoutRecentIrrigation();
        when(waterNeedService.needLiters(zone, 5d)).thenReturn(1000d);
        when(scheduleRepository.save(any(IrrigationSchedule.class))).thenAnswer(call -> call.getArgument(0));
        when(zoneBlockMappingService.blockCodesForZone(zone.getId())).thenReturn(List.of("A"));
        when(plantsGrowthPort.growthForBlock("A"))
                .thenReturn(List.of(new PlantsGrowthPort.BlockGrowth("A", "full production", 7)));

        Map<String, Object> report = service(new WaterIntegrationProperties(6.0, 7.5, 5.0, 180.0, 0.8, 15, 10,
                5.0, 24, 2.0, 0.5, 1.0, 500.0, 30, 15, 1.0, 1.0, 1.2, 7)).trigger();

        Map<?, ?> created = (Map<?, ?>) ((List<?>) report.get("created")).get(0);
        assertEquals(1200d, created.get("planned_liters"));
        assertEquals("full production", created.get("growth_phase"));
        assertEquals(1.2d, created.get("phase_factor"));
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

    /** A zone below every seasonal threshold, with no open schedule and no irrigation in the last 24 h. */
    private Zone stubDryZoneWithoutRecentIrrigation() {
        Zone zone = stubSingleZone();
        zone.setId(UUID.randomUUID());
        when(agroWeatherService.et0Between(any(), any())).thenReturn(5d);
        when(soilMoistureRepository.findLatestMoisturePercentBefore(eq(zone.getId()), any())).thenReturn(10d);
        when(scheduleRepository.findByZoneId(zone.getId())).thenReturn(List.of());
        when(logRepository.findForZoneBetween(eq(zone.getId()), any(), any())).thenReturn(List.of());
        return zone;
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
        return service(WaterIntegrationProperties.defaults());
    }

    private IrrigationAutomationService service(WaterIntegrationProperties properties) {
        PlantsIntegrationService plants = new PlantsIntegrationService(zoneBlockMappingService, fieldZoneRepository,
                rainfallService, waterNeedService, agroWeatherService, plantsGrowthPort, properties);
        return new IrrigationAutomationService(fieldZoneRepository, soilMoistureRepository, scheduleRepository,
                logRepository, agroWeatherService, alertService, fieldRepository, rainfallService,
                unavailabilityService, plants, properties);
    }
}
