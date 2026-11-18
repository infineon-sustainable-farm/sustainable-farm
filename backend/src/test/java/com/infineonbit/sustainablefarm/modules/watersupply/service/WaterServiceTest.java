package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.modules.watersupply.config.WaterIntegrationProperties;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterConsumptionCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterConsumptionResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterQualityCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterSourceCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterSourceUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterConsumption;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterLevelHistory;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterSource;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.farm.api.FarmDirectory;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.NotificationRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.UserRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterConsumptionRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterLevelHistoryRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterQualityTestRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSourceRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WaterServiceTest {
    @Mock
    private FarmDirectory farmDirectory;

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

    @Mock
    private ZoneBlockResolver zoneBlockResolver;

    @Mock
    private WaterLevelHistoryRepository levelHistoryRepository;

    @Test
    void createSourceRejectsUnknownFarm() {
        Long farmId = 111L;
        WaterSourceCreateRequest source = new WaterSourceCreateRequest(farmId, "Source", "tank", 100.0, 50.0, null, null);
        when(farmDirectory.farmExists(farmId)).thenReturn(false);
        WaterService service = service();

        assertThrows(NotFoundException.class, () -> service.createSource(source));
        verify(waterSourceRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createConsumptionRejectsSourceFromAnotherFarm() {
        Long farmId = 112L;
        Long sourceId = 113L;
        WaterSource source = new WaterSource();
        source.setFarmId(121L);
        WaterConsumptionCreateRequest consumption = new WaterConsumptionCreateRequest(farmId, sourceId, 10.0, java.time.Instant.now(), null, null);
        when(farmDirectory.farmExists(farmId)).thenReturn(true);
        when(waterSourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        WaterService service = service();

        assertThrows(IllegalArgumentException.class, () -> service.createConsumption(consumption));
        verify(waterConsumptionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createQualityTestRejectsUnknownSource() {
        Long sourceId = 114L;
        WaterQualityCreateRequest test = new WaterQualityCreateRequest(sourceId, 7.0, null, null, null, null, java.time.Instant.now());
        when(waterSourceRepository.findById(sourceId)).thenReturn(Optional.empty());
        WaterService service = service();

        assertThrows(NotFoundException.class, () -> service.createQualityTest(test));
        verify(waterQualityTestRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createConsumptionNormalizesLotAndActivityAndExposesTheBlock() {
        Long farmId = 115L;
        Long sourceId = 116L;
        Long zoneId = 117L;
        WaterSource source = new WaterSource();
        source.setFarmId(farmId);
        when(farmDirectory.farmExists(farmId)).thenReturn(true);
        when(waterSourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(waterConsumptionRepository.save(any(WaterConsumption.class))).thenAnswer(call -> call.getArgument(0));
        when(zoneBlockResolver.blockCodesForZone(zoneId)).thenReturn(List.of("A"));

        WaterConsumptionResponse saved = service().createConsumption(new WaterConsumptionCreateRequest(
                farmId, sourceId, 120.0, Instant.now(), null, zoneId, "  LOT-001 ", "washing"));

        assertEquals("LOT-001", saved.lotId());
        assertEquals("WASHING", saved.activity());
        assertEquals("A", saved.blockCode());
        // The zone is now passed to the quota check, so zone quotas are evaluated too.
        verify(waterQuotaService).checkThresholds(org.mockito.ArgumentMatchers.eq(farmId),
                org.mockito.ArgumentMatchers.eq(zoneId), any());
    }

    @Test
    void createConsumptionRejectsUnknownActivity() {
        Long farmId = 118L;
        WaterConsumptionCreateRequest request = new WaterConsumptionCreateRequest(
                farmId, 119L, 10.0, Instant.now(), null, null, "LOT-1", "painting");

        assertThrows(IllegalArgumentException.class, () -> service().createConsumption(request));
        verify(waterConsumptionRepository, never()).save(any());
    }

    @Test
    void updateSourceRecordsLevelHistoryOnlyWhenTheLevelChanges() {
        Long sourceId = 119L;
        WaterSource source = new WaterSource();
        source.setId(sourceId);
        source.setCurrentLevelLiters(500.0);
        when(waterSourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(waterSourceRepository.save(source)).thenReturn(source);
        WaterService service = service();

        service.updateSource(sourceId, new WaterSourceUpdateRequest(null, null, null, null, 500.0, null, null));
        verify(levelHistoryRepository, never()).save(any());

        service.updateSource(sourceId, new WaterSourceUpdateRequest(null, null, null, null, 420.0, null, null));
        ArgumentCaptor<WaterLevelHistory> history = ArgumentCaptor.forClass(WaterLevelHistory.class);
        verify(levelHistoryRepository).save(history.capture());
        assertEquals(sourceId, history.getValue().getSourceId());
        assertEquals(420.0, history.getValue().getLevelLiters());
    }

    @Test
    void levelHistoryRejectsAnInvertedWindow() {
        Long sourceId = 120L;
        when(waterSourceRepository.findById(sourceId)).thenReturn(Optional.of(new WaterSource()));

        assertThrows(IllegalArgumentException.class, () -> service().sourceLevelHistory(
                sourceId, Instant.parse("2026-10-02T00:00:00Z"), Instant.parse("2026-10-01T00:00:00Z")));
    }

    private WaterService service() {
        return new WaterService(
                farmDirectory,
                waterSourceRepository,
                waterConsumptionRepository,
                waterQualityTestRepository,
                notificationRepository,
                userRepository,
                waterQuotaService,
                zoneBlockResolver,
                WaterIntegrationProperties.defaults(),
                levelHistoryRepository);
    }
}