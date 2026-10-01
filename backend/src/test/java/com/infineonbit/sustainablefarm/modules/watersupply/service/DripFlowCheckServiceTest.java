package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationLog;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Notification;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Zone;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IrrigationLogRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterConsumptionRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldZoneRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Detection de colmatage / fuite (module 6.2) : le diagnostic ne doit se prononcer que lorsque
 * les trois informations existent (reseau decrit, arrosage reel, mesure de debit rattachee).
 */
@ExtendWith(MockitoExtension.class)
class DripFlowCheckServiceTest {

    @Mock
    private FieldZoneRepository fieldZoneRepository;

    @Mock
    private WaterConsumptionRepository consumptionRepository;

    @Mock
    private IrrigationLogRepository logRepository;

    @Mock
    private AlertService alertService;

    @Test
    void reportsPossibleCloggingWhenMeasuredVolumeIsBelowNinetyPercent() {
        UUID zoneId = stubZoneWithRating();
        stubTwoHourCycle(zoneId);
        when(consumptionRepository.sumConsumptionByZoneIdBetween(any(), any(), eq(zoneId))).thenReturn(300d);
        when(alertService.raiseOnce(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(new Notification()));

        Map<String, Object> result = service().check(zoneId);

        // Reseau de 200 L/h sur 2 h = 400 L attendus ; 300 L mesures = 75 %.
        assertEquals("possible_clogging", result.get("status"));
        assertEquals(400d, result.get("theoretical_liters"));
        assertEquals(0.75d, result.get("ratio"));
        assertEquals(true, result.get("alert_raised"));
    }

    @Test
    void reportsPossibleLeakWhenMeasuredVolumeIsAboveHundredTenPercent() {
        UUID zoneId = stubZoneWithRating();
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
        UUID zoneId = stubZoneWithRating();
        stubTwoHourCycle(zoneId);
        when(consumptionRepository.sumConsumptionByZoneIdBetween(any(), any(), eq(zoneId))).thenReturn(400d);

        Map<String, Object> result = service().check(zoneId);

        assertEquals("ok", result.get("status"));
        assertEquals(false, result.get("alert_raised"));
    }

    @Test
    void doesNotConcludeWithoutEmitterData() {
        UUID zoneId = UUID.randomUUID();
        Zone zone = new Zone();
        zone.setName("Zone A");
        when(fieldZoneRepository.findById(zoneId)).thenReturn(Optional.of(zone));

        Map<String, Object> result = service().check(zoneId);

        assertEquals("network_not_described", result.get("status"));
    }

    @Test
    void doesNotConcludeWithoutIrrigationCycle() {
        UUID zoneId = stubZoneWithRating();
        when(logRepository.findForZoneBetween(eq(zoneId), any(), any())).thenReturn(List.of());

        Map<String, Object> result = service().check(zoneId);

        assertEquals("no_irrigation", result.get("status"));
    }

    @Test
    void doesNotConcludeWithoutMeasurement() {
        UUID zoneId = stubZoneWithRating();
        stubTwoHourCycle(zoneId);
        when(consumptionRepository.sumConsumptionByZoneIdBetween(any(), any(), eq(zoneId))).thenReturn(0d);

        Map<String, Object> result = service().check(zoneId);

        assertEquals("no_measurement", result.get("status"));
    }

    @Test
    void rejectsUnknownZone() {
        UUID zoneId = UUID.randomUUID();
        when(fieldZoneRepository.findById(zoneId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service().check(zoneId));
    }

    /** Zone goutte-a-goutte de 100 goutteurs a 2 L/h : 200 L/h attendus. */
    private UUID stubZoneWithRating() {
        UUID zoneId = UUID.randomUUID();
        Zone zone = new Zone();
        zone.setName("Zone A");
        zone.setIrrigationMethod("drip");
        zone.setEmitterCount(100);
        zone.setEmitterNominalFlowLh(2.0);
        when(fieldZoneRepository.findById(zoneId)).thenReturn(Optional.of(zone));
        return zoneId;
    }

    private void stubTwoHourCycle(UUID zoneId) {
        Instant now = Instant.now();
        IrrigationLog log = new IrrigationLog();
        log.setActualStartTime(now.minus(3, ChronoUnit.HOURS));
        log.setActualEndTime(now.minus(1, ChronoUnit.HOURS));
        when(logRepository.findForZoneBetween(eq(zoneId), any(), any())).thenReturn(List.of(log));
    }

    private DripFlowCheckService service() {
        return new DripFlowCheckService(fieldZoneRepository, consumptionRepository, logRepository, alertService);
    }
}
