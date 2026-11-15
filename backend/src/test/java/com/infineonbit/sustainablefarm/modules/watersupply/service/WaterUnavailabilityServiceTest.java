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
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Field;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationSchedule;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterUnavailability;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Zone;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FarmRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldZoneRepository;
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
    private FarmRepository farmRepository;

    @Mock
    private WaterSourceRepository sourceRepository;

    @Mock
    private FieldZoneRepository zoneRepository;

    @Mock
    private FieldRepository fieldRepository;

    @Mock
    private IrrigationScheduleRepository scheduleRepository;

    @Mock
    private IrrigationService irrigationService;

    @Mock
    private AlertService alertService;

    @Test
    void createRejectsAPeriodEndingBeforeItStarts() {
        assertThrows(IllegalArgumentException.class,
                () -> service().create(request("zone", UUID.randomUUID(), END, START, null, null)));
        verify(outageRepository, never()).save(any());
    }

    @Test
    void createRejectsAnUnknownScope() {
        assertThrows(IllegalArgumentException.class,
                () -> service().create(request("field", UUID.randomUUID(), START, END, null, null)));
    }

    @Test
    void createRejectsAnUnknownStatus() {
        assertThrows(IllegalArgumentException.class,
                () -> service().create(request("zone", UUID.randomUUID(), START, END, "done", null)));
    }

    @Test
    void createRejectsAnUnknownTarget() {
        UUID zoneId = UUID.randomUUID();
        when(zoneRepository.existsById(zoneId)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service().create(request("zone", zoneId, START, END, null, null)));
    }

    @Test
    void createRejectsAnOverlapOnTheSameScope() {
        UUID sourceId = UUID.randomUUID();
        when(sourceRepository.existsById(sourceId)).thenReturn(true);
        when(outageRepository.overlaps("source", sourceId, START, END)).thenReturn(List.of(existing(UUID.randomUUID())));

        assertThrows(IllegalArgumentException.class, () -> service().create(request("source", sourceId, START, END, null, null)));
        verify(outageRepository, never()).save(any());
    }

    @Test
    void aCancelledPeriodNeverConflicts() {
        UUID sourceId = UUID.randomUUID();
        when(sourceRepository.existsById(sourceId)).thenReturn(true);
        when(outageRepository.save(any(WaterUnavailability.class))).thenAnswer(call -> call.getArgument(0));

        WaterUnavailabilityResponse saved = service().create(request("SOURCE", sourceId, START, END, "Cancelled", null));

        assertEquals("cancelled", saved.status());
        verify(outageRepository, never()).overlaps(anyString(), any(), any(), any());
    }

    @Test
    void aFarmWidePeriodPostponesTheOpenIrrigationsOfEveryZoneOfTheFarm() {
        UUID farmId = UUID.randomUUID();
        Field field = new Field();
        field.setId(UUID.randomUUID());
        Zone zone = new Zone();
        zone.setId(UUID.randomUUID());
        IrrigationSchedule schedule = new IrrigationSchedule();
        schedule.setId(UUID.randomUUID());
        when(farmRepository.existsById(farmId)).thenReturn(true);
        when(outageRepository.overlaps("farm", farmId, START, END)).thenReturn(List.of());
        when(outageRepository.save(any(WaterUnavailability.class))).thenAnswer(call -> call.getArgument(0));
        when(fieldRepository.findByFarmId(farmId)).thenReturn(List.of(field));
        when(zoneRepository.findByFieldId(field.getId())).thenReturn(List.of(zone));
        when(scheduleRepository.findOpenForZoneBetween(zone.getId(), START, END)).thenReturn(List.of(schedule));

        service().create(new WaterUnavailabilityRequest("farm", farmId, START, END, "pump repair", null, true));

        verify(irrigationService).postpone(schedule.getId(), "Water unavailability: pump repair");
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(alertService).raise(eq("warning"), eq("Water unavailability planned"), message.capture(), anyString());
        assertTrue(message.getValue().contains("1 irrigation(s) postponed"));
    }

    @Test
    void updateDoesNotConflictWithItself() {
        UUID id = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        WaterUnavailability current = existing(id);
        when(outageRepository.findById(id)).thenReturn(Optional.of(current));
        when(zoneRepository.existsById(zoneId)).thenReturn(true);
        when(outageRepository.overlaps("zone", zoneId, START, END)).thenReturn(List.of(current));
        when(outageRepository.save(current)).thenReturn(current);

        WaterUnavailabilityResponse updated = service().update(id, request("zone", zoneId, START, END, "active", null));

        assertEquals("active", updated.status());
    }

    private static WaterUnavailabilityRequest request(String scope, UUID scopeId, Instant start, Instant end,
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
        return new WaterUnavailabilityService(outageRepository, farmRepository, sourceRepository, zoneRepository,
                fieldRepository, scheduleRepository, irrigationService, alertService);
    }
}
