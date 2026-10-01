package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.ZoneCreateRequest;
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
}