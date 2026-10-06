package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.RainwaterHarvestRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.RainwaterHarvestResponse;
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
        when(waterSourceRepository.findById(sourceId)).thenReturn(Optional.of(new WaterSource()));
        when(harvestRepository.save(any(RainwaterHarvest.class))).thenAnswer(call -> call.getArgument(0));

        RainwaterHarvestResponse saved = service().create(request(sourceId, 10.0, 20.0, 0.5));

        assertEquals(100.0, saved.harvestedLiters());
    }

    @Test
    void updateRecalculatesUsingExistingValuesWhenPayloadIsPartial() {
        UUID sourceId = UUID.randomUUID();
        UUID harvestId = UUID.randomUUID();
        RainwaterHarvest existing = harvest(sourceId, 10.0, 20.0, 0.5);
        existing.setHarvestedLiters(100.0);
        when(harvestRepository.findById(harvestId)).thenReturn(Optional.of(existing));
        when(waterSourceRepository.findById(sourceId)).thenReturn(Optional.of(new WaterSource()));
        when(harvestRepository.save(existing)).thenReturn(existing);

        RainwaterHarvestResponse updated = service().update(harvestId,
                new RainwaterHarvestRequest(null, null, 30.0, null, null, null));

        assertEquals(150.0, updated.harvestedLiters());
    }

    @Test
    void createRejectsUnknownSource() {
        UUID sourceId = UUID.randomUUID();
        when(waterSourceRepository.findById(sourceId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service().create(request(sourceId, 10.0, 20.0, 0.5)));
        verify(harvestRepository, never()).save(any(RainwaterHarvest.class));
    }

    /** Client request: id and creation date never come from the client. */
    private RainwaterHarvestRequest request(UUID sourceId, double area, double rainfall, double coefficient) {
        return new RainwaterHarvestRequest(sourceId, area, rainfall, coefficient, null, null);
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