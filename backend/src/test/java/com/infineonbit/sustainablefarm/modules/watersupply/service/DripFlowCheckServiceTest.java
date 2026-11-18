package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationLog;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Notification;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IrrigationLogRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterConsumptionRepository;
import com.infineonbit.sustainablefarm.modules.farm.api.FarmDirectory;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Clogging / leak detection (module 6.2): the diagnosis must only be pronounced when
 * the three pieces of information exist (network described, actual irrigation, flow measurement attached).
 */
@ExtendWith(MockitoExtension.class)
class DripFlowCheckServiceTest {

    @Mock
    private FarmDirectory farmDirectory;

    @Mock
    private WaterConsumptionRepository consumptionRepository;

    @Mock
    private IrrigationLogRepository logRepository;

    @Mock
    private AlertService alertService;

    @Test
    void reportsPossibleCloggingWhenMeasuredVolumeIsBelowNinetyPercent() {
        Long zoneId = stubZoneWithRating();
        stubTwoHourCycle(zoneId);
        when(consumptionRepository.sumConsumptionByZoneIdBetween(any(), any(), eq(zoneId))).thenReturn(300d);
        when(alertService.raiseOnce(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(new Notification()));

        Map<String, Object> result = service().check(zoneId);

        // Network of 200 L/h over 2 h = 400 L expected; 300 L measured = 75 %.
        assertEquals("possible_clogging", result.get("status"));
        assertEquals(400d, result.get("theoretical_liters"));
        assertEquals(0.75d, result.get("ratio"));
        assertEquals(true, result.get("alert_raised"));
    }

    @Test
    void reportsPossibleLeakWhenMeasuredVolumeIsAboveHundredTenPercent() {
        Long zoneId = stubZoneWithRating();
        stubTwoHourCycle(zoneId);
        when(consumptionRepository.sumConsumptionByZoneIdBetween(any(), any(), eq(zoneId))).thenReturn(500d);
        when(alertService.raiseOnce(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(new Notification()));

        Map<String, Object> result = service().check(zoneId);

        assertEquals("possible_leak", result.get("status"));
        assertEquals(1.25d, result.get("ratio"));
    }

    @Test
    void reportsOkWhenMeasuredVolumeMatchesTheNetworkRating() {
        Long zoneId = stubZoneWithRating();
        stubTwoHourCycle(zoneId);
        when(consumptionRepository.sumConsumptionByZoneIdBetween(any(), any(), eq(zoneId))).thenReturn(400d);

        Map<String, Object> result = service().check(zoneId);

        assertEquals("ok", result.get("status"));
        assertEquals(false, result.get("alert_raised"));
    }

    @Test
    void doesNotConcludeWithoutEmitterData() {
        Long zoneId = 5L;
        when(farmDirectory.findZone(zoneId)).thenReturn(Optional.of(TestZones.zone().id(zoneId).name("Zone A").build()));

        Map<String, Object> result = service().check(zoneId);

        assertEquals("network_not_described", result.get("status"));
    }

    @Test
    void doesNotConcludeWithoutIrrigationCycle() {
        Long zoneId = stubZoneWithRating();
        when(logRepository.findForZoneBetween(eq(zoneId), any(), any())).thenReturn(List.of());

        Map<String, Object> result = service().check(zoneId);

        assertEquals("no_irrigation", result.get("status"));
    }

    @Test
    void doesNotConcludeWithoutMeasurement() {
        Long zoneId = stubZoneWithRating();
        stubTwoHourCycle(zoneId);
        when(consumptionRepository.sumConsumptionByZoneIdBetween(any(), any(), eq(zoneId))).thenReturn(0d);

        Map<String, Object> result = service().check(zoneId);

        assertEquals("no_measurement", result.get("status"));
    }

    @Test
    void rejectsUnknownZone() {
        Long zoneId = 6L;
        when(farmDirectory.findZone(zoneId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service().check(zoneId));
    }

    /** Drip zone of 100 emitters at 2 L/h: 200 L/h expected. */
    private Long stubZoneWithRating() {
        Long zoneId = 7L;
        when(farmDirectory.findZone(zoneId)).thenReturn(Optional.of(TestZones.zone().id(zoneId).name("Zone A")
                .irrigationMethod("drip").emitterCount(100).emitterNominalFlowLh(2.0).build()));
        return zoneId;
    }

    private void stubTwoHourCycle(Long zoneId) {
        Instant now = Instant.now();
        IrrigationLog log = new IrrigationLog();
        log.setActualStartTime(now.minus(3, ChronoUnit.HOURS));
        log.setActualEndTime(now.minus(1, ChronoUnit.HOURS));
        when(logRepository.findForZoneBetween(eq(zoneId), any(), any())).thenReturn(List.of(log));
    }

    private DripFlowCheckService service() {
        return new DripFlowCheckService(farmDirectory, consumptionRepository, logRepository, alertService);
    }
}
