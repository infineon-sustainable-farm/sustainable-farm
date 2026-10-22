package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.RainwaterHarvest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterSource;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.RainwaterHarvestRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSourceRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rainfall measured by the rain gauges of the farms' sources ({@code rain} telemetry, stored in
 * {@code rainwater_harvests.rainfall_mm}).
 *
 * <p>Rainfall in mm is a property of the rain event, not of the tank: two tanks of the same farm
 * report the same rain. Summing every reading would double it, so the farm rainfall is the highest
 * cumulative value among its gauges (robust to a gauge that missed a reading).</p>
 */
@Service
@Transactional(readOnly = true)
public class RainfallService {

    /** Rainfall over a window: {@code rainfallMm} is the max of the per-gauge totals. */
    public record RainfallSummary(double rainfallMm, Map<UUID, Double> bySource, List<RainwaterHarvest> readings) {
        static final RainfallSummary NONE = new RainfallSummary(0d, Map.of(), List.of());
    }

    private final WaterSourceRepository sourceRepository;
    private final RainwaterHarvestRepository harvestRepository;

    public RainfallService(WaterSourceRepository sourceRepository, RainwaterHarvestRepository harvestRepository) {
        this.sourceRepository = sourceRepository;
        this.harvestRepository = harvestRepository;
    }

    /** Rainfall of the given farms over [from, to[. */
    public RainfallSummary forFarms(Collection<UUID> farmIds, Instant from, Instant to) {
        if (farmIds.isEmpty()) {
            return RainfallSummary.NONE;
        }
        List<UUID> sourceIds = sourceRepository.findByFarmIdIn(farmIds).stream().map(WaterSource::getId).toList();
        if (sourceIds.isEmpty()) {
            return RainfallSummary.NONE;
        }
        List<RainwaterHarvest> readings = harvestRepository.findBySourcesBetween(sourceIds, from, to);
        Map<UUID, Double> bySource = new LinkedHashMap<>();
        for (RainwaterHarvest reading : readings) {
            bySource.merge(reading.getSourceId(), reading.getRainfallMm() == null ? 0d : reading.getRainfallMm(), Double::sum);
        }
        double rainfall = bySource.values().stream().mapToDouble(Double::doubleValue).max().orElse(0d);
        return new RainfallSummary(rainfall, bySource, readings);
    }
}
