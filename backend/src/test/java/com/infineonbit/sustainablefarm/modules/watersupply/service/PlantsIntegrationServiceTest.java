package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.modules.watersupply.config.WaterIntegrationProperties;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.BlockIrrigationNeedRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.RainwaterHarvest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Zone;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldZoneRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Plants integration (2.2, 2.3, 2.4): rainfall and irrigation need expressed per block. */
@ExtendWith(MockitoExtension.class)
class PlantsIntegrationServiceTest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 13);

    @Mock
    private ZoneBlockMappingService zoneBlockMappingService;

    @Mock
    private FieldZoneRepository zoneRepository;

    @Mock
    private RainfallService rainfallService;

    @Mock
    private WaterNeedService waterNeedService;

    @Mock
    private AgroWeatherService agroWeatherService;

    @Mock
    private PlantsGrowthPort plantsGrowthPort;

    @Test
    void rainfallIsReadOnTheGaugesOfTheFarmsOwningTheBlock() {
        UUID farmId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        RainwaterHarvest reading = new RainwaterHarvest();
        reading.setSourceId(sourceId);
        reading.setRainfallMm(12.34);
        reading.setCaptureDate(Instant.parse("2026-10-12T08:00:00Z"));
        when(zoneBlockMappingService.farmIdsForBlock(null, "A")).thenReturn(List.of(farmId));
        when(rainfallService.forFarms(eq(List.of(farmId)), eq(Instant.parse("2026-10-01T00:00:00Z")),
                eq(Instant.parse("2026-10-14T00:00:00Z"))))
                .thenReturn(new RainfallService.RainfallSummary(12.34, Map.of(sourceId, 12.34), List.of(reading)));

        Map<String, Object> rainfall = service().rainfall("a", null, LocalDate.of(2026, 10, 1), DAY);

        assertEquals("A", rainfall.get("blockCode"));
        assertEquals(12.3, rainfall.get("rainfallMm"));
        assertEquals("farm", rainfall.get("spatialResolution"));
        assertEquals(1, rainfall.get("gaugeCount"));
        assertEquals(1, ((List<?>) rainfall.get("readings")).size());
    }

    @Test
    void rainfallOfAnUnmappedBlockIsNotFound() {
        when(zoneBlockMappingService.farmIdsForBlock(null, "Z")).thenReturn(List.of());

        assertThrows(NotFoundException.class, () -> service().rainfall("Z", null, DAY, DAY));
    }

    @Test
    void rainfallRejectsAnInvertedPeriod() {
        assertThrows(IllegalArgumentException.class, () -> service().rainfall("A", null, DAY, DAY.minusDays(1)));
    }

    @Test
    void irrigationNeedAppliesTheFactorOfTheRequestedPhase() {
        Zone zone = stubBlockZone("A");
        when(waterNeedService.needLiters(zone, 4d)).thenReturn(1000d);

        Map<String, Object> need = service(factors(0.6, 0.9, 1.1)).irrigationNeed(
                new BlockIrrigationNeedRequest("A", null, 120, "GRADUAL_PRODUCTION", 4, DAY));

        assertEquals(1000L, need.get("baseNeedLiters"));
        assertEquals("gradual production", need.get("growthPhase"));
        assertEquals("request", need.get("growthPhaseSource"));
        assertEquals(900L, need.get("needLiters"));
        assertEquals(120, need.get("treeCount"));
        assertEquals(List.of(), need.get("warnings"));
    }

    @Test
    void irrigationNeedReadsThePhaseFromThePlantsCalendarWhenNotGiven() {
        Zone zone = stubBlockZone("A");
        when(waterNeedService.needLiters(zone, 4d)).thenReturn(1000d);
        when(plantsGrowthPort.growthForBlock("A"))
                .thenReturn(List.of(new PlantsGrowthPort.BlockGrowth("A", "establishment", 1)));

        Map<String, Object> need = service(factors(0.6, 0.9, 1.1)).irrigationNeed(
                new BlockIrrigationNeedRequest("a", null, null, null, null, DAY));

        assertEquals("plants_calendar", need.get("growthPhaseSource"));
        assertEquals(600L, need.get("needLiters"));
    }

    @Test
    void conflictingPhasesInThePlantsCalendarLeaveTheNeedUnadjusted() {
        Zone zone = stubBlockZone("A");
        when(waterNeedService.needLiters(zone, 4d)).thenReturn(1000d);
        when(plantsGrowthPort.growthForBlock("A")).thenReturn(List.of(
                new PlantsGrowthPort.BlockGrowth("A", "establishment", 1),
                new PlantsGrowthPort.BlockGrowth("A", "full production", 8)));

        Map<String, Object> need = service(factors(0.6, 0.9, 1.1)).irrigationNeed(
                new BlockIrrigationNeedRequest("A", null, null, null, null, DAY));

        assertEquals(1000L, need.get("needLiters"));
        assertEquals("unknown", need.get("growthPhaseSource"));
        assertTrue(((List<?>) need.get("warnings")).contains("growth_phase_unknown_no_adjustment"));
    }

    @Test
    void defaultFactorsAreFlaggedAsNotValidated() {
        Zone zone = stubBlockZone("A");
        when(waterNeedService.needLiters(zone, 4d)).thenReturn(1000d);

        Map<String, Object> need = service().irrigationNeed(
                new BlockIrrigationNeedRequest("A", null, null, "full production", null, DAY));

        assertEquals(1000L, need.get("needLiters"));
        assertTrue(((List<?>) need.get("warnings")).contains("phase_factors_not_validated"));
    }

    @Test
    void irrigationNeedRejectsAnUnknownPhase() {
        stubBlockZoneWithoutWeather("A");

        assertThrows(IllegalArgumentException.class, () -> service().irrigationNeed(
                new BlockIrrigationNeedRequest("A", null, null, "flowering", null, DAY)));
    }

    @Test
    void phaseAdjustmentNeedsExactlyOneBlockOnTheZone() {
        UUID zoneId = UUID.randomUUID();
        when(zoneBlockMappingService.blockCodesForZone(zoneId)).thenReturn(List.of("A", "B"));

        PlantsIntegrationService.PhaseAdjustment adjustment = service(factors(0.6, 0.9, 1.1)).phaseAdjustmentForZone(zoneId);

        assertEquals(1.0, adjustment.factor());
        assertEquals("unknown", adjustment.source());
    }

    private Zone stubBlockZone(String blockCode) {
        Zone zone = stubBlockZoneWithoutWeather(blockCode);
        when(agroWeatherService.et0Between(DAY, DAY)).thenReturn(4d);
        when(zoneRepository.findById(zone.getId())).thenReturn(Optional.of(zone));
        return zone;
    }

    private Zone stubBlockZoneWithoutWeather(String blockCode) {
        Zone zone = new Zone();
        zone.setId(UUID.randomUUID());
        when(zoneBlockMappingService.zoneIdsForBlock(any(), eq(blockCode))).thenReturn(List.of(zone.getId()));
        return zone;
    }

    private static WaterIntegrationProperties factors(double establishment, double gradual, double full) {
        return new WaterIntegrationProperties(6.0, 7.5, 5.0, 180.0, 0.8, 15, 10, 5.0, 24, 2.0, 0.5, 1.0, 500.0,
                30, 15, establishment, gradual, full);
    }

    private PlantsIntegrationService service() {
        return service(WaterIntegrationProperties.defaults());
    }

    private PlantsIntegrationService service(WaterIntegrationProperties properties) {
        return new PlantsIntegrationService(zoneBlockMappingService, zoneRepository, rainfallService, waterNeedService,
                agroWeatherService, plantsGrowthPort, properties);
    }
}
