package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.IrrigationLogCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.IrrigationLogResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.IrrigationScheduleCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationLog;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationSchedule;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IrrigationLogRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IrrigationScheduleRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.ZoneRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IrrigationServiceTest {
    @Mock
    private IrrigationScheduleRepository scheduleRepository;

    @Mock
    private IrrigationLogRepository logRepository;

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private AgroWeatherService agroWeatherService;

    @Mock
    private AlertService alertService;

    @Test
    void createScheduleRejectsUnknownZone() {
        UUID zoneId = UUID.randomUUID();
        IrrigationScheduleCreateRequest schedule = new IrrigationScheduleCreateRequest(
            zoneId, Instant.now(), 10, 20.0, null, UUID.randomUUID());
        when(zoneRepository.findById(zoneId)).thenReturn(Optional.empty());
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

    private IrrigationService service() {
        return new IrrigationService(scheduleRepository, logRepository, zoneRepository, agroWeatherService, alertService);
    }
}
