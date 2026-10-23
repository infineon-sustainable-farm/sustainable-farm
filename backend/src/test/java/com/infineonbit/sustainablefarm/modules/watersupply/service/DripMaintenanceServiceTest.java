package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.DripMaintenanceLogRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.DripMaintenanceLog;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Zone;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.DripMaintenanceLogRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldZoneRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DripMaintenanceServiceTest {
    @Mock
    private DripMaintenanceLogRepository logRepository;

    @Mock
    private FieldZoneRepository fieldZoneRepository;

    @Test
    void createRejectsUnknownZone() {
        UUID zoneId = UUID.randomUUID();
        DripMaintenanceLogRequest request = new DripMaintenanceLogRequest(
                zoneId, Instant.now(), "inspection", null, null, null, null, null, null);
        when(fieldZoneRepository.findById(zoneId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service().create(request));
        verify(logRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createRejectsAMissingMaintenanceDate() {
        UUID zoneId = UUID.randomUUID();
        when(fieldZoneRepository.findById(zoneId)).thenReturn(Optional.of(new Zone()));
        DripMaintenanceLogRequest request = new DripMaintenanceLogRequest(
                zoneId, null, "inspection", null, null, null, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> service().create(request));
        verify(logRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void calendarIsOverdueWhenTheLastInspectionIsTooOld() {
        Zone zone = stubZone();
        // Inspection done 10 days ago for a 7-day periodicity: 3 days of delay.
        when(logRepository.findAll()).thenReturn(List.of(intervention(zone.getId(), "inspection", 10)));

        Map<String, Object> entry = entryFor(service().schedule(), "inspection");

        assertEquals(LocalDate.now(ZoneOffset.UTC).minusDays(10).toString(), entry.get("last_done"));
        assertEquals(LocalDate.now(ZoneOffset.UTC).minusDays(3).toString(), entry.get("next_due"));
        assertEquals(-3L, entry.get("days_remaining"));
        assertEquals("overdue", entry.get("status"));
    }

    @Test
    void calendarPlansTheNextMonthlyFlush() {
        Zone zone = stubZone();
        when(logRepository.findAll()).thenReturn(List.of(intervention(zone.getId(), "filter cleaning", 1)));

        Map<String, Object> flush = entryFor(service().schedule(), "flush");
        Map<String, Object> filter = entryFor(service().schedule(), "filter_cleaning");

        // The manually typed "filter cleaning" label is recognized as filter cleaning.
        assertEquals(13L, filter.get("days_remaining"));
        assertEquals("planned", filter.get("status"));
        // The flush was never done: it is due today.
        assertEquals("never_done", flush.get("status"));
        assertEquals(LocalDate.now(ZoneOffset.UTC).toString(), flush.get("next_due"));
    }

    @Test
    void calendarIsEmptyWithoutZone() {
        when(fieldZoneRepository.findAll()).thenReturn(List.of());

        assertTrue(service().schedule().isEmpty());
    }

    private Zone stubZone() {
        Zone zone = new Zone();
        zone.setId(UUID.randomUUID());
        zone.setName("Zone A");
        when(fieldZoneRepository.findAll()).thenReturn(List.of(zone));
        return zone;
    }

    private DripMaintenanceLog intervention(UUID zoneId, String type, int daysAgo) {
        DripMaintenanceLog log = new DripMaintenanceLog();
        log.setZoneId(zoneId);
        log.setMaintenanceType(type);
        log.setMaintenanceDate(dateDaysAgo(daysAgo));
        return log;
    }

    /** Start of a UTC day, {@code daysAgo} days ago: deterministic dates for the test. */
    private Instant dateDaysAgo(int daysAgo) {
        return LocalDate.now(ZoneOffset.UTC).minusDays(daysAgo).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private Map<String, Object> entryFor(List<Map<String, Object>> calendar, String taskType) {
        return calendar.stream()
                .filter(entry -> taskType.equals(entry.get("task_type")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing task " + taskType));
    }

    private DripMaintenanceService service() {
        return new DripMaintenanceService(logRepository, fieldZoneRepository);
    }
}