package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.FieldCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.ZoneCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FarmRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.ZoneRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FarmServiceTest {
    @Mock
    private FarmRepository farmRepository;

    @Mock
    private FieldRepository fieldRepository;

    @Mock
    private ZoneRepository zoneRepository;

    @Test
    void createFieldRejectsUnknownFarm() {
        UUID farmId = UUID.randomUUID();
        FieldCreateRequest field = new FieldCreateRequest(farmId, "Field", 1.0, null, null, null);
        when(farmRepository.findById(farmId)).thenReturn(Optional.empty());
        FarmService service = new FarmService(farmRepository, fieldRepository, zoneRepository);

        assertThrows(NotFoundException.class, () -> service.createField(field));
        verify(fieldRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createZoneRejectsUnknownField() {
        UUID fieldId = UUID.randomUUID();
        ZoneCreateRequest zone = new ZoneCreateRequest(fieldId, "Zone", 1.0, null);
        when(fieldRepository.findById(fieldId)).thenReturn(Optional.empty());
        FarmService service = new FarmService(farmRepository, fieldRepository, zoneRepository);

        assertThrows(NotFoundException.class, () -> service.createZone(zone));
        verify(zoneRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}