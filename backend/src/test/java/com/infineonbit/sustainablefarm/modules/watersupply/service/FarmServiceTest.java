package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.FarmCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Farm;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FarmRepository;
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

    @Test
    void createFarmPersistsTheRequestedDetails() {
        FarmCreateRequest request = new FarmCreateRequest("North Farm", "Demo", "Road 1", 10.0, -4.0, 12.5);
        when(farmRepository.save(org.mockito.ArgumentMatchers.any(Farm.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        FarmService service = new FarmService(farmRepository);

        var response = service.createFarm(request);

        assertEquals("North Farm", response.name());
        assertEquals("Demo", response.description());
        assertEquals("Road 1", response.address());
        assertEquals(12.5, response.areaHectares());
    }

    @Test
    void getFarmRejectsAnUnknownId() {
        UUID farmId = UUID.randomUUID();
        when(farmRepository.findById(farmId)).thenReturn(Optional.empty());
        FarmService service = new FarmService(farmRepository);

        assertThrows(ResourceNotFoundException.class, () -> service.getFarm(farmId));
    }
}