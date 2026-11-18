package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterUnavailabilityRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterUnavailabilityResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationSchedule;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterUnavailability;
import com.infineonbit.sustainablefarm.modules.farm.api.ZoneDto;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.farm.api.FarmDirectory;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IrrigationScheduleRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSourceRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterUnavailabilityRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Planned water unavailability (3.3): validation, overlap and postponement of the impacted irrigations. */
@ExtendWith(MockitoExtension.class)
class WaterUnavailabilityServiceTest {
    private static final Instant START = Instant.parse("2026-10-20T08:00:00Z");
    private static final Instant END = Instant.parse("2026-10-20T12:00:00Z");

    @Mock
    private WaterUnavailabilityRepository outageRepository;

    @Mock
    private WaterSourceRepository sourceRepository;

    @Mock
    private FarmDirectory farmDirectory;

    @Mock
    private IrrigationScheduleRepository scheduleRepository;

    @Mock
    private IrrigationService irrigationService;

    @Mock
    private AlertService alertService;

    @Test
    void createRejectsAPeriodEndingBeforeItStarts() {
        assertThrows(IllegalArgumentException.class,
                () -> service().create(request("zone", 1L, END, START, null, null)));
        verify(outageRepository, never()).save(any());
    }

    @Test
    void createRejectsAnUnknownScope() {
        assertThrows(IllegalArgumentException.class,
                () -> service().create(request("field", 1L, START, END, null, null)));
    }

    @Test
    void createRejectsAnUnknownStatus() {
        assertThrows(IllegalArgumentException.class,
                () -> service().create(request("zone", 1L, START, END, "done", null)));
    }

    @Test
    void createRejectsAnUnknownTarget() {
        Long zoneId = 134L;
        when(farmDirectory.zoneExists(zoneId)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service().create(request("zone", zoneId, START, END, null, null)));
    }

    @Test
    void createRejectsAnOverlapOnTheSameScope() {
        Long sourceId = 135L;
        when(sourceRepository.existsById(sourceId)).thenReturn(true);
        when(outageRepository.overlaps("source", sourceId, START, END)).thenReturn(List.of(existing(UUID.randomUUID())));

        assertThrows(IllegalArgumentException.class, () -> service().create(request("source", sourceId, START, END, null, null)));
        verify(outageRepository, never()).save(any());
    }

    @Test
    void aCancelledPeriodNeverConflicts() {
        Long sourceId = 136L;
        when(sourceRepository.existsById(sourceId)).thenReturn(true);
        when(outageRepository.save(any(WaterUnavailability.class))).thenAnswer(call -> call.getArgument(0));

        WaterUnavailabilityResponse saved = service().create(request("SOURCE", sourceId, START, END, "Cancelled", null));

        assertEquals("cancelled", saved.status());
        verify(outageRepository, never()).overlaps(anyString(), any(), any(), any());
    }

    @Test
    void aFarmWidePeriodPostponesTheOpenIrrigationsOfEveryZoneOfTheFarm() {
        Long farmId = 137L;
        ZoneDto zone = TestZones.zone().id(11L).blockId(12L).farmId(farmId).build();
        IrrigationSchedule schedule = new IrrigationSchedule();
        schedule.setId(UUID.randomUUID());
        when(farmDirectory.farmExists(farmId)).thenReturn(true);
        when(outageRepository.overlaps("farm", farmId, START, END)).thenReturn(List.of());
        when(outageRepository.save(any(WaterUnavailability.class))).thenAnswer(call -> call.getArgument(0));
        when(farmDirectory.listZonesOfFarm(farmId)).thenReturn(List.of(zone));
        when(scheduleRepository.findOpenForZoneBetween(zone.id(), START, END)).thenReturn(List.of(schedule));

        service().create(new WaterUnavailabilityRequest("farm", farmId, START, END, "pump repair", null, true));

        verify(irrigationService).postpone(schedule.getId(), "Water unavailability: pump repair");
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(alertService).raise(eq("warning"), eq("Water unavailability planned"), message.capture(), anyString());
        assertTrue(message.getValue().contains("1 irrigation(s) postponed"));
    }

    @Test
    void updateDoesNotConflictWithItself() {
        UUID id = UUID.randomUUID();
        Long zoneId = 138L;
        WaterUnavailability current = existing(id);
        when(outageRepository.findById(id)).thenReturn(Optional.of(current));
        when(farmDirectory.zoneExists(zoneId)).thenReturn(true);
        when(outageRepository.overlaps("zone", zoneId, START, END)).thenReturn(List.of(current));
        when(outageRepository.save(current)).thenReturn(current);

        WaterUnavailabilityResponse updated = service().update(id, request("zone", zoneId, START, END, "active", null));

        assertEquals("active", updated.status());
    }

    private static WaterUnavailabilityRequest request(String scope, Long scopeId, Instant start, Instant end,
                                                      String status, Boolean postpone) {
        return new WaterUnavailabilityRequest(scope, scopeId, start, end, "maintenance", status, postpone);
    }

    private static WaterUnavailability existing(UUID id) {
        WaterUnavailability outage = new WaterUnavailability();
        outage.setId(id);
        outage.setStatus("planned");
        return outage;
    }

    private WaterUnavailabilityService service() {
        return new WaterUnavailabilityService(outageRepository, sourceRepository, farmDirectory,
                scheduleRepository, irrigationService, alertService);
    }
}
