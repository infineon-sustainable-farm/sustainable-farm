package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.RainwaterHarvest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterSource;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.RainwaterHarvestRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSourceRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RainwaterHarvestServiceTest {
    @Mock
    private RainwaterHarvestRepository harvestRepository;

    @Mock
    private WaterSourceRepository waterSourceRepository;

    @Test
    void createCalculatesHarvestedLiters() {
        UUID sourceId = UUID.randomUUID();
        RainwaterHarvest harvest = harvest(sourceId, 10.0, 20.0, 0.5);
        when(waterSourceRepository.findById(sourceId)).thenReturn(Optional.of(new WaterSource()));
        when(harvestRepository.save(harvest)).thenReturn(harvest);

        RainwaterHarvest saved = service().create(harvest);

        assertEquals(100.0, saved.getHarvestedLiters());
    }

    @Test
    void updateRecalculatesUsingExistingValuesWhenPayloadIsPartial() {
        UUID sourceId = UUID.randomUUID();
        UUID harvestId = UUID.randomUUID();
        RainwaterHarvest existing = harvest(sourceId, 10.0, 20.0, 0.5);
        existing.setHarvestedLiters(100.0);
        RainwaterHarvest payload = new RainwaterHarvest();
        payload.setRainfallMm(30.0);
        when(harvestRepository.findById(harvestId)).thenReturn(Optional.of(existing));
        when(waterSourceRepository.findById(sourceId)).thenReturn(Optional.of(new WaterSource()));
        when(harvestRepository.save(existing)).thenReturn(existing);

        RainwaterHarvest updated = service().update(harvestId, payload);

        assertEquals(150.0, updated.getHarvestedLiters());
    }

    @Test
    void createRejectsUnknownSource() {
        UUID sourceId = UUID.randomUUID();
        RainwaterHarvest harvest = harvest(sourceId, 10.0, 20.0, 0.5);
        when(waterSourceRepository.findById(sourceId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service().create(harvest));
        verify(harvestRepository, never()).save(harvest);
    }

    private RainwaterHarvest harvest(UUID sourceId, double area, double rainfall, double coefficient) {
        RainwaterHarvest harvest = new RainwaterHarvest();
        harvest.setSourceId(sourceId);
        harvest.setCatchmentAreaM2(area);
        harvest.setRainfallMm(rainfall);
        harvest.setRunoffCoefficient(coefficient);
        return harvest;
    }

    private RainwaterHarvestService service() {
        return new RainwaterHarvestService(harvestRepository, waterSourceRepository);
    }
}