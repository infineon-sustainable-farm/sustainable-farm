package com.infineonbit.sustainablefarm.modules.farm.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.farm.dto.FieldCreateRequest;
import com.infineonbit.sustainablefarm.modules.farm.dto.ZoneCreateRequest;
import com.infineonbit.sustainablefarm.modules.farm.repository.BlockRepository;
import com.infineonbit.sustainablefarm.modules.farm.repository.FarmRepository;
import com.infineonbit.sustainablefarm.modules.farm.repository.FarmZoneRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FarmServiceTest {
    @Mock
    private FarmRepository farmRepository;

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private FarmZoneRepository zoneRepository;

    @Test
    void createZoneRejectsUnknownField() {
        ZoneCreateRequest zone = new ZoneCreateRequest(7L, "Zone", 1.0, null, null, null, null);
        when(blockRepository.findById(7L)).thenReturn(Optional.empty());
        FarmService service = new FarmService(farmRepository, blockRepository, zoneRepository);

        assertThrows(ResourceNotFoundException.class, () -> service.createZone(zone));
        verify(zoneRepository, never()).save(any());
    }

    @Test
    void createFieldRejectsUnknownFarm() {
        FieldCreateRequest request = new FieldCreateRequest(3L, "Field", 1.0, null, null, null);
        when(farmRepository.findById(3L)).thenReturn(Optional.empty());
        BlockService service = new BlockService(farmRepository, blockRepository);

        assertThrows(ResourceNotFoundException.class, () -> service.createField(request));
        verify(blockRepository, never()).save(any());
    }
}
