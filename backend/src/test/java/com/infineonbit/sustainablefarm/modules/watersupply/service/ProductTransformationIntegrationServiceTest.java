package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.modules.watersupply.config.WaterIntegrationProperties;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.AvailabilityForecastRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.LotUsageRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterConsumption;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterQualityTest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterSource;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IrrigationScheduleRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterConsumptionRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterQualityTestRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSourceRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

/** Product Transformation integration (4.1-4.5). */
@ExtendWith(MockitoExtension.class)
class ProductTransformationIntegrationServiceTest {
    private static final Instant START = Instant.parse("2026-10-13T08:00:00Z");
    private static final Instant END = Instant.parse("2026-10-13T10:00:00Z");

    @Mock
    private WaterConsumptionRepository consumptionRepository;

    @Mock
    private WaterQualityTestRepository qualityRepository;

    @Mock
    private WaterSourceRepository sourceRepository;

    @Mock
    private IrrigationScheduleRepository scheduleRepository;

    @Mock
    private AlertService alertService;

    @Test
    @SuppressWarnings("unchecked")
    void lotUsageNeverTakesOverAConsumptionOfAnotherLot() {
        UUID sourceId = UUID.randomUUID();
        WaterConsumption free = consumption(sourceId, 100.0, null);
        WaterConsumption sameLot = consumption(sourceId, 50.0, "LOT-1");
        WaterConsumption otherLot = consumption(sourceId, 30.0, "LOT-9");
        when(consumptionRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(free, sameLot, otherLot));

        Map<String, Object> response = service().attachLotUsage(
                new LotUsageRequest(" LOT-1 ", "washing", START, END, sourceId, null));

        assertEquals(2, response.get("attachedCount"));
        assertEquals(150.0, response.get("attachedLiters"));
        assertEquals(1, response.get("skippedOtherLotCount"));
        assertEquals(List.of("window_overlaps_other_lot"), response.get("warnings"));
        assertEquals("LOT-1", free.getLotId());
        assertEquals("WASHING", free.getActivity());
        assertEquals("LOT-9", otherLot.getLotId());
        ArgumentCaptor<List<WaterConsumption>> saved = ArgumentCaptor.forClass(List.class);
        verify(consumptionRepository).saveAll(saved.capture());
        assertEquals(List.of(free, sameLot), saved.getValue());
    }

    @Test
    void lotUsageWithoutFilterOrConsumptionIsReportedNotSilent() {
        Map<String, Object> response = service().attachLotUsage(new LotUsageRequest("LOT-2", null, START, END, null, null));

        assertEquals(0, response.get("attachedCount"));
        assertEquals(List.of("no_consumption_found", "no_source_or_zone_filter"), response.get("warnings"));
    }

    @Test
    void lotUsageRejectsAnInvertedWindowAndAnUnknownActivity() {
        assertThrows(IllegalArgumentException.class,
                () -> service().attachLotUsage(new LotUsageRequest("LOT-3", null, END, START, null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> service().attachLotUsage(new LotUsageRequest("LOT-3", "painting", START, END, null, null)));
    }

    @Test
    void byLotAggregatesPerActivity() {
        WaterConsumption washing = consumption(UUID.randomUUID(), 250.0, "LOT-1");
        washing.setActivity("WASHING");
        WaterConsumption unknown = consumption(UUID.randomUUID(), 20.0, "LOT-1");
        when(consumptionRepository.findByLotIdOrderByConsumptionDateAsc("LOT-1")).thenReturn(List.of(washing, unknown));

        Map<String, Object> response = service().byLot("LOT-1");

        assertEquals(270.0, response.get("totalLiters"));
        assertEquals(Map.of("WASHING", 250.0, "OTHER", 20.0), response.get("byActivity"));
    }

    @Test
    void complianceUsesTheLotSourcesAndWindowByDefault() {
        UUID sourceId = UUID.randomUUID();
        WaterConsumption first = consumption(sourceId, 10.0, "LOT-1");
        WaterConsumption last = consumption(sourceId, 10.0, "LOT-1");
        last.setConsumptionDate(END);
        when(consumptionRepository.findByLotIdOrderByConsumptionDateAsc("LOT-1")).thenReturn(List.of(first, last));
        when(qualityRepository.findBySourcesBetween(List.of(sourceId), START.minus(7, ChronoUnit.DAYS), END))
                .thenReturn(List.of(test(sourceId, 7.0, 1.0), test(sourceId, 8.2, 1.0)));

        Map<String, Object> response = service().qualityCompliance("LOT-1", null, null);

        assertEquals("non_compliant", response.get("status"));
        assertEquals(false, response.get("compliant"));
        assertEquals(List.of(sourceId), response.get("sourceIds"));
    }

    @Test
    void complianceWithoutAnyTestIsNotACompliance() {
        when(qualityRepository.findBetween(START.minus(7, ChronoUnit.DAYS), END)).thenReturn(List.of());

        Map<String, Object> response = service().qualityCompliance(null, START, END);

        assertEquals("no_test", response.get("status"));
        assertEquals(false, response.get("compliant"));
        assertNull(response.get("lotId"));
    }

    @Test
    void aTestMadeShortlyBeforeTheLotStillCounts() {
        UUID sourceId = UUID.randomUUID();
        stubLotWindow("LOT-1", sourceId);
        when(qualityRepository.findBySourcesBetween(List.of(sourceId), START.minus(7, ChronoUnit.DAYS), END))
                .thenReturn(List.of(test(sourceId, 7.0, 1.0, START.minus(3, ChronoUnit.HOURS))));

        Map<String, Object> response = service().qualityCompliance("LOT-1", null, null);

        assertEquals("compliant", response.get("status"));
        assertEquals(7, response.get("validityDays"));
    }

    @Test
    void onlyTheLastTestBeforeTheLotIsDecisive() {
        UUID sourceId = UUID.randomUUID();
        stubLotWindow("LOT-1", sourceId);
        // Failed two days before, fixed and passed the day before: the water was compliant at the start.
        when(qualityRepository.findBySourcesBetween(List.of(sourceId), START.minus(7, ChronoUnit.DAYS), END)).thenReturn(List.of(
                test(sourceId, 8.4, 1.0, START.minus(2, ChronoUnit.DAYS)),
                test(sourceId, 7.0, 1.0, START.minus(1, ChronoUnit.DAYS))));

        Map<String, Object> response = service().qualityCompliance("LOT-1", null, null);

        assertEquals("compliant", response.get("status"));
        assertEquals(1, ((List<?>) response.get("tests")).size());
    }

    @Test
    void everySourceUsedByTheLotNeedsItsOwnTest() {
        UUID tested = UUID.randomUUID();
        UUID untested = UUID.randomUUID();
        WaterConsumption first = consumption(tested, 10.0, "LOT-1");
        WaterConsumption last = consumption(untested, 10.0, "LOT-1");
        last.setConsumptionDate(END);
        when(consumptionRepository.findByLotIdOrderByConsumptionDateAsc("LOT-1")).thenReturn(List.of(first, last));
        when(qualityRepository.findBySourcesBetween(List.of(tested, untested), START.minus(7, ChronoUnit.DAYS), END))
                .thenReturn(List.of(test(tested, 7.0, 1.0)));

        Map<String, Object> response = service().qualityCompliance("LOT-1", null, null);

        // A change of source needs a test of the new source, whatever the time elapsed.
        assertEquals("no_test", response.get("status"));
        List<?> sources = (List<?>) response.get("sources");
        assertEquals("compliant", ((Map<?, ?>) sources.get(0)).get("status"));
        assertEquals("no_test", ((Map<?, ?>) sources.get(1)).get("status"));
    }

    @Test
    void complianceNeedsAWindowWhenTheLotHasNoConsumption() {
        when(consumptionRepository.findByLotIdOrderByConsumptionDateAsc("LOT-0")).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> service().qualityCompliance("LOT-0", null, null));
    }

    @Test
    void forecastAddsBatchAndFarmIrrigationDemandAndRaisesATightBufferAlert() {
        WaterSource source = source(2000.0);
        when(sourceRepository.findById(source.getId())).thenReturn(Optional.of(source));
        when(scheduleRepository.sumPlannedDemandForFarmBetween(eq(source.getFarmId()), any(), any())).thenReturn(400d);
        Instant tomorrow = Instant.now().plus(1, ChronoUnit.DAYS);

        Map<String, Object> response = service().forecast(new AvailabilityForecastRequest(source.getId(), 7, null, List.of(
                new AvailabilityForecastRequest.Batch("LOT-1", "washing", tomorrow, 500.0),
                new AvailabilityForecastRequest.Batch("LOT-2", "cutting", Instant.now().plus(30, ChronoUnit.DAYS), 100.0))));

        // Washing 500 kg x 2 L/kg = 1000 L; the cutting batch is outside the 7-day horizon.
        assertEquals(1000L, response.get("batchDemandLiters"));
        assertEquals(400L, response.get("irrigationDemandLiters"));
        assertEquals(600L, response.get("availableAfterForecastLiters"));
        assertEquals("ok", response.get("bufferStatus"));
        verify(alertService, never()).raiseOnce(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void forecastFlagsAnInsufficientSource() {
        WaterSource source = source(300.0);
        when(sourceRepository.findById(source.getId())).thenReturn(Optional.of(source));

        Map<String, Object> response = service().forecast(new AvailabilityForecastRequest(source.getId(), null, false, List.of(
                new AvailabilityForecastRequest.Batch("LOT-1", "DRYER_CLEANING", null, null))));

        // Dryer cleaning: 1 m3 per lot, irrigation excluded on request.
        assertEquals(-700L, response.get("availableAfterForecastLiters"));
        assertEquals("insufficient", response.get("bufferStatus"));
        verify(scheduleRepository, never()).sumPlannedDemandForFarmBetween(any(), any(), any());
        verify(alertService).raiseOnce(eq("warning"), eq("Water buffer insufficient - Tank"), anyString(), anyString());
    }

    @Test
    void forecastWarnsWhenAnActivityHasNoCoefficient() {
        WaterSource source = source(5000.0);
        when(sourceRepository.findById(source.getId())).thenReturn(Optional.of(source));

        Map<String, Object> response = service().forecast(new AvailabilityForecastRequest(source.getId(), 1, false, List.of(
                new AvailabilityForecastRequest.Batch("LOT-1", "other", null, 10.0))));

        assertTrue(((List<?>) response.get("warnings")).contains("no_coefficient_for_activity_other"));
    }

    private static WaterConsumption consumption(UUID sourceId, double liters, String lotId) {
        WaterConsumption consumption = new WaterConsumption();
        consumption.setId(UUID.randomUUID());
        consumption.setSourceId(sourceId);
        consumption.setConsumptionLiters(liters);
        consumption.setConsumptionDate(START);
        consumption.setLotId(lotId);
        return consumption;
    }

    private static WaterQualityTest test(UUID sourceId, double ph, double turbidity) {
        return test(sourceId, ph, turbidity, START);
    }

    private static WaterQualityTest test(UUID sourceId, double ph, double turbidity, Instant at) {
        WaterQualityTest test = new WaterQualityTest();
        test.setSourceId(sourceId);
        test.setPh(ph);
        test.setTurbidityNtu(turbidity);
        test.setTestDate(at);
        return test;
    }

    /** A lot consumed from START to END on one source. */
    private void stubLotWindow(String lotId, UUID sourceId) {
        WaterConsumption first = consumption(sourceId, 10.0, lotId);
        WaterConsumption last = consumption(sourceId, 10.0, lotId);
        last.setConsumptionDate(END);
        when(consumptionRepository.findByLotIdOrderByConsumptionDateAsc(lotId)).thenReturn(List.of(first, last));
    }

    private static WaterSource source(double level) {
        WaterSource source = new WaterSource();
        source.setId(UUID.randomUUID());
        source.setFarmId(UUID.randomUUID());
        source.setName("Tank");
        source.setCurrentLevelLiters(level);
        return source;
    }

    private ProductTransformationIntegrationService service() {
        return new ProductTransformationIntegrationService(consumptionRepository, qualityRepository, sourceRepository,
                scheduleRepository, alertService, WaterIntegrationProperties.defaults());
    }
}
