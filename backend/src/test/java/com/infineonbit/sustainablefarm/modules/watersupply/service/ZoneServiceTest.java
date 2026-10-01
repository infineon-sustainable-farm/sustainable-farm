package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.ZoneCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.ZoneUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Zone;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldZoneRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ZoneServiceTest {
    @Mock
    private FieldRepository fieldRepository;

    @Mock
    private FieldZoneRepository fieldZoneRepository;

    @Test
    void createZoneRejectsUnknownField() {
        UUID fieldId = UUID.randomUUID();
        ZoneCreateRequest request = new ZoneCreateRequest(fieldId, "Zone", 1.0, null);
        when(fieldRepository.findById(fieldId)).thenReturn(Optional.empty());
        ZoneService service = new ZoneService(fieldRepository, fieldZoneRepository);

        assertThrows(ResourceNotFoundException.class, () -> service.createZone(request));
        verify(fieldZoneRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getZoneRejectsAnUnknownId() {
        UUID zoneId = UUID.randomUUID();
        when(fieldZoneRepository.findById(zoneId)).thenReturn(Optional.empty());
        ZoneService service = new ZoneService(fieldRepository, fieldZoneRepository);

        assertThrows(ResourceNotFoundException.class, () -> service.getZone(zoneId));
    }

    @Test
    void updateZoneKeepsFieldsThatWereOmitted() {
        UUID zoneId = UUID.randomUUID();
        UUID fieldId = UUID.randomUUID();
        Zone zone = new Zone();
        zone.setId(zoneId);
        zone.setFieldId(fieldId);
        zone.setName("North zone");
        zone.setAreaHectares(2.5);
        zone.setIrrigationMethod("drip");
        when(fieldZoneRepository.findById(zoneId)).thenReturn(Optional.of(zone));
        when(fieldZoneRepository.save(any(Zone.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ZoneService service = new ZoneService(fieldRepository, fieldZoneRepository);

        var response = service.updateZone(zoneId, new ZoneUpdateRequest(null, null, null, null));

        assertEquals(fieldId, response.fieldId());
        assertEquals("North zone", response.name());
        assertEquals(2.5, response.areaHectares());
        assertEquals("drip", response.irrigationMethod());
    }

    @Test
    void updateZoneRejectsAnUnknownReplacementField() {
        UUID zoneId = UUID.randomUUID();
        UUID fieldId = UUID.randomUUID();
        Zone zone = new Zone();
        zone.setId(zoneId);
        zone.setFieldId(UUID.randomUUID());
        when(fieldZoneRepository.findById(zoneId)).thenReturn(Optional.of(zone));
        when(fieldRepository.findById(fieldId)).thenReturn(Optional.empty());
        ZoneService service = new ZoneService(fieldRepository, fieldZoneRepository);

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateZone(zoneId, new ZoneUpdateRequest(fieldId, null, null, null)));
        verify(fieldZoneRepository, never()).save(any(Zone.class));
    }
}