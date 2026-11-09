package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.RainwaterHarvest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterSource;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.RainwaterHarvestRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSourceRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Farm rainfall from the rain gauges: never the sum of several gauges measuring the same rain. */
@ExtendWith(MockitoExtension.class)
class RainfallServiceTest {
    private static final Instant FROM = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-10-08T00:00:00Z");

    @Mock
    private WaterSourceRepository sourceRepository;

    @Mock
    private RainwaterHarvestRepository harvestRepository;

    @Test
    void twoGaugesOfTheSameFarmGiveTheHighestCumulativeNotTheSum() {
        UUID farmId = UUID.randomUUID();
        WaterSource tankA = source(farmId);
        WaterSource tankB = source(farmId);
        when(sourceRepository.findByFarmIdIn(List.of(farmId))).thenReturn(List.of(tankA, tankB));
        when(harvestRepository.findBySourcesBetween(List.of(tankA.getId(), tankB.getId()), FROM, TO)).thenReturn(List.of(
                harvest(tankA, 12.0), harvest(tankB, 12.0), harvest(tankA, 3.0), harvest(tankB, null)));

        RainfallService.RainfallSummary summary = service().forFarms(List.of(farmId), FROM, TO);

        assertEquals(15.0, summary.rainfallMm());
        assertEquals(15.0, summary.bySource().get(tankA.getId()));
        assertEquals(12.0, summary.bySource().get(tankB.getId()));
        assertEquals(4, summary.readings().size());
    }

    @Test
    void aFarmWithoutSourceHasNoRainAndNoGaugeQuery() {
        UUID farmId = UUID.randomUUID();
        when(sourceRepository.findByFarmIdIn(List.of(farmId))).thenReturn(List.of());

        assertEquals(0d, service().forFarms(List.of(farmId), FROM, TO).rainfallMm());
        verify(harvestRepository, never()).findBySourcesBetween(any(), any(), any());
    }

    @Test
    void noFarmMeansNoQueryAtAll() {
        assertEquals(0d, service().forFarms(List.of(), FROM, TO).rainfallMm());
        verify(sourceRepository, never()).findByFarmIdIn(any());
    }

    private static WaterSource source(UUID farmId) {
        WaterSource source = new WaterSource();
        source.setId(UUID.randomUUID());
        source.setFarmId(farmId);
        return source;
    }

    private static RainwaterHarvest harvest(WaterSource source, Double rainfallMm) {
        RainwaterHarvest harvest = new RainwaterHarvest();
        harvest.setSourceId(source.getId());
        harvest.setRainfallMm(rainfallMm);
        harvest.setCaptureDate(FROM.plusSeconds(3600));
        return harvest;
    }

    private RainfallService service() {
        return new RainfallService(sourceRepository, harvestRepository);
    }
}
