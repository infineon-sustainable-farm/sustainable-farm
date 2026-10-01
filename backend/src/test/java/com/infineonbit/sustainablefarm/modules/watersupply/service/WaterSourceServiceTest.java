package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterSourceCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FarmRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSourceRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WaterSourceServiceTest {
    @Mock
    private FarmRepository farmRepository;

    @Mock
    private WaterSourceRepository waterSourceRepository;

    @Test
    void createSourceRejectsUnknownFarm() {
        UUID farmId = UUID.randomUUID();
        WaterSourceCreateRequest request = new WaterSourceCreateRequest(
                farmId, "Source", "tank", 100.0, 50.0, null, null);
        when(farmRepository.findById(farmId)).thenReturn(Optional.empty());
        WaterSourceService service = new WaterSourceService(farmRepository, waterSourceRepository);

        assertThrows(ResourceNotFoundException.class, () -> service.createSource(request));
        verify(waterSourceRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}