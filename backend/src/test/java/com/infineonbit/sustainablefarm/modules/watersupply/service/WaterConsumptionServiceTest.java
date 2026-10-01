package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterConsumptionCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Farm;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterSource;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FarmRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterConsumptionRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSourceRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WaterConsumptionServiceTest {
    @Mock
    private FarmRepository farmRepository;

    @Mock
    private WaterSourceRepository waterSourceRepository;

    @Mock
    private WaterConsumptionRepository waterConsumptionRepository;

    @Test
    void createConsumptionRejectsSourceFromAnotherFarm() {
        UUID farmId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        WaterSource source = new WaterSource();
        source.setFarmId(UUID.randomUUID());
        when(farmRepository.findById(farmId)).thenReturn(Optional.of(new Farm()));
        when(waterSourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        WaterConsumptionService service = new WaterConsumptionService(
                farmRepository, waterSourceRepository, waterConsumptionRepository);
        WaterConsumptionCreateRequest request = new WaterConsumptionCreateRequest(
                farmId, sourceId, 10.0, Instant.now(), null, null);

        assertThrows(BusinessRuleException.class, () -> service.createConsumption(request));
        verify(waterConsumptionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}