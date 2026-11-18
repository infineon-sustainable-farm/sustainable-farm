package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.IrrigationLogCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.IrrigationLogResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.IrrigationScheduleCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.IrrigationScheduleResponse;
import com.infineonbit.sustainablefarm.core.web.PageResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationLog;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationSchedule;
import com.infineonbit.sustainablefarm.modules.farm.api.ZoneDto;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IrrigationLogRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IrrigationScheduleRepository;
import com.infineonbit.sustainablefarm.modules.farm.api.FarmDirectory;
import com.infineonbit.sustainablefarm.modules.staff.api.InMemoryPersonDirectory;
import com.infineonbit.sustainablefarm.modules.watersupply.config.SystemUsers;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class IrrigationServiceTest {
    @Mock
    private IrrigationScheduleRepository scheduleRepository;

    @Mock
    private IrrigationLogRepository logRepository;

    @Mock
    private FarmDirectory farmDirectory;

    @Mock
    private AgroWeatherService agroWeatherService;

    @Mock
    private AlertService alertService;

    @Mock
    private ZoneBlockResolver zoneBlockResolver;

    @Test
    void createScheduleRejectsUnknownZone() {
        Long zoneId = 101L;
        IrrigationScheduleCreateRequest schedule = new IrrigationScheduleCreateRequest(
            zoneId, Instant.now(), 10, 20.0, null, 7L);
        when(farmDirectory.findZone(zoneId)).thenReturn(Optional.empty());
        IrrigationService service = service();

        assertThrows(NotFoundException.class, () -> service.createSchedule(schedule));
        verify(scheduleRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createLogRejectsUnknownSchedule() {
        UUID scheduleId = UUID.randomUUID();
        IrrigationLogCreateRequest log = new IrrigationLogCreateRequest(
            scheduleId, Instant.now(), null, 0.0, "started");
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.empty());
        IrrigationService service = service();

        assertThrows(NotFoundException.class, () -> service.createLog(log));
        verify(logRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void stopCompletesMostRecentStartedLog() {
        UUID scheduleId = UUID.randomUUID();
        IrrigationSchedule schedule = new IrrigationSchedule();
        schedule.setWaterQuantityLiters(42.0);
        IrrigationLog started = new IrrigationLog();
        started.setScheduleId(scheduleId);
        started.setActualStartTime(Instant.parse("2026-01-01T10:00:00Z"));
        started.setStatus("started");
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(logRepository.findFirstByScheduleIdAndStatusOrderByActualStartTimeDesc(scheduleId, "started"))
                .thenReturn(Optional.of(started));
        when(logRepository.save(started)).thenReturn(started);
        IrrigationService service = service();

        IrrigationLogResponse completed = service.stop(scheduleId);

        assertEquals("completed", schedule.getStatus());
        assertEquals("completed", completed.status());
        assertEquals(42.0, completed.waterUsedLiters());
        assertNotNull(completed.actualEndTime());
        assertEquals(Instant.parse("2026-01-01T10:00:00Z"), completed.actualStartTime());
        verify(logRepository).save(started);
    }

    @Test
    void irrigationStatusReportsTheRunningScheduleAndItsActualStart() {
        Long zoneId = 102L;
        IrrigationSchedule running = schedule(zoneId, "running", Instant.parse("2026-10-13T10:00:00Z"));
        IrrigationLog log = new IrrigationLog();
        log.setActualStartTime(Instant.parse("2026-10-13T10:02:00Z"));
        when(farmDirectory.findZone(zoneId)).thenReturn(Optional.of(TestZones.zone().build()));
        when(scheduleRepository.findByZoneIdAndStatusIgnoreCase(zoneId, "running")).thenReturn(List.of(running));
        when(logRepository.findFirstByScheduleIdOrderByActualStartTimeDesc(running.getId())).thenReturn(Optional.of(log));

        Map<String, Object> status = service().irrigationStatus(zoneId);

        assertEquals(true, status.get("active"));
        assertEquals("running", status.get("status"));
        assertEquals(running.getId(), status.get("scheduleId"));
        assertEquals(Instant.parse("2026-10-13T10:02:00Z"), status.get("lastActivationAt"));
        assertEquals(300.0, status.get("plannedLiters"));
    }

    @Test
    void irrigationStatusFallsBackToTheLatestScheduleWhenNothingRuns() {
        Long zoneId = 103L;
        IrrigationSchedule older = schedule(zoneId, "completed", Instant.parse("2026-10-10T06:00:00Z"));
        IrrigationSchedule latest = schedule(zoneId, "scheduled", Instant.parse("2026-10-14T06:00:00Z"));
        when(farmDirectory.findZone(zoneId)).thenReturn(Optional.of(TestZones.zone().build()));
        when(scheduleRepository.findByZoneIdAndStatusIgnoreCase(zoneId, "running")).thenReturn(List.of());
        when(scheduleRepository.findByZoneId(zoneId)).thenReturn(List.of(older, latest));
        when(logRepository.findFirstByScheduleIdOrderByActualStartTimeDesc(latest.getId())).thenReturn(Optional.empty());

        Map<String, Object> status = service().irrigationStatus(zoneId);

        assertEquals(false, status.get("active"));
        assertEquals("scheduled", status.get("status"));
        // Never started: the planned start is the best known activation time.
        assertEquals(Instant.parse("2026-10-14T06:00:00Z"), status.get("lastActivationAt"));
    }

    @Test
    void irrigationStatusOfAZoneWithoutScheduleIsNone() {
        Long zoneId = 104L;
        when(farmDirectory.findZone(zoneId)).thenReturn(Optional.of(TestZones.zone().build()));
        when(scheduleRepository.findByZoneIdAndStatusIgnoreCase(zoneId, "running")).thenReturn(List.of());
        when(scheduleRepository.findByZoneId(zoneId)).thenReturn(List.of());

        Map<String, Object> status = service().irrigationStatus(zoneId);

        assertEquals("none", status.get("status"));
        assertNull(status.get("lastActivationAt"));
    }

    @Test
    void irrigationStatusRejectsUnknownZone() {
        Long zoneId = 105L;
        when(farmDirectory.findZone(zoneId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service().irrigationStatus(zoneId));
    }

    @Test
    void blockFilterWithoutMappedZoneReturnsAnEmptyPageWithoutQuerying() {
        when(zoneBlockResolver.zoneIdsForBlock(null, "Z")).thenReturn(List.of());

        PageResponse<IrrigationScheduleResponse> page = service().findSchedules(PageRequest.of(0, 10), null, null, "Z");

        assertTrue(page.content().isEmpty());
        assertEquals(0, page.totalElements());
        verify(scheduleRepository, never()).findByZoneIdIn(any(), any());
    }

    @Test
    void scheduleCarriesTheBlockCodeOnlyWhenTheZoneMapsToOneBlock() {
        Long single = 106L;
        Long multiple = 107L;
        when(scheduleRepository.findAll()).thenReturn(List.of(schedule(single, "scheduled", Instant.now()),
                schedule(multiple, "scheduled", Instant.now())));
        when(zoneBlockResolver.blockCodesForZones(any())).thenReturn(Map.of(single, List.of("A"), multiple, List.of("B", "C")));

        List<IrrigationScheduleResponse> schedules = service().findSchedules();

        assertEquals("A", schedules.get(0).blockCode());
        assertFalse(schedules.get(0).multipleBlockCodes());
        assertNull(schedules.get(1).blockCode());
        assertTrue(schedules.get(1).multipleBlockCodes());
    }

    private static IrrigationSchedule schedule(Long zoneId, String status, Instant start) {
        IrrigationSchedule schedule = new IrrigationSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setZoneId(zoneId);
        schedule.setStatus(status);
        schedule.setStartTime(start);
        schedule.setWaterQuantityLiters(300.0);
        return schedule;
    }

    private IrrigationService service() {
        return new IrrigationService(scheduleRepository, logRepository, farmDirectory, agroWeatherService,
                alertService, zoneBlockResolver, new SystemUsers(new InMemoryPersonDirectory()));
    }
}
