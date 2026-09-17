package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterConsumptionCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterQualityCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterSourceCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterSource;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FarmRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.NotificationRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.UserRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterConsumptionRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterQualityTestRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSourceRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WaterServiceTest {
    @Mock
    private FarmRepository farmRepository;

    @Mock
    private WaterSourceRepository waterSourceRepository;

    @Mock
    private WaterConsumptionRepository waterConsumptionRepository;

    @Mock
    private WaterQualityTestRepository waterQualityTestRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WaterQuotaService waterQuotaService;

    @Test
    void createSourceRejectsUnknownFarm() {
        UUID farmId = UUID.randomUUID();
        WaterSourceCreateRequest source = new WaterSourceCreateRequest(farmId, "Source", "tank", 100.0, 50.0, null, null);
        when(farmRepository.findById(farmId)).thenReturn(Optional.empty());
        WaterService service = service();

        assertThrows(NotFoundException.class, () -> service.createSource(source));
        verify(waterSourceRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createConsumptionRejectsSourceFromAnotherFarm() {
        UUID farmId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        WaterSource source = new WaterSource();
        source.setFarmId(UUID.randomUUID());
        WaterConsumptionCreateRequest consumption = new WaterConsumptionCreateRequest(farmId, sourceId, 10.0, java.time.Instant.now(), null);
        when(farmRepository.findById(farmId)).thenReturn(Optional.of(new com.infineonbit.sustainablefarm.modules.watersupply.entity.Farm()));
        when(waterSourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        WaterService service = service();

        assertThrows(IllegalArgumentException.class, () -> service.createConsumption(consumption));
        verify(waterConsumptionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createQualityTestRejectsUnknownSource() {
        UUID sourceId = UUID.randomUUID();
        WaterQualityCreateRequest test = new WaterQualityCreateRequest(sourceId, 7.0, null, null, null, null, java.time.Instant.now());
        when(waterSourceRepository.findById(sourceId)).thenReturn(Optional.empty());
        WaterService service = service();

        assertThrows(NotFoundException.class, () -> service.createQualityTest(test));
        verify(waterQualityTestRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private WaterService service() {
        return new WaterService(
                farmRepository,
                waterSourceRepository,
                waterConsumptionRepository,
                waterQualityTestRepository,
                notificationRepository,
                userRepository,
                waterQuotaService);
    }
}