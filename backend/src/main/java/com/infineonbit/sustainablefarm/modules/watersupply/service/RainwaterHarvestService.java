package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.RainwaterHarvest;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.RainwaterHarvestRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSourceRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RainwaterHarvestService {
    private final RainwaterHarvestRepository harvestRepository;
    private final WaterSourceRepository waterSourceRepository;

    public RainwaterHarvestService(RainwaterHarvestRepository harvestRepository, WaterSourceRepository waterSourceRepository) {
        this.harvestRepository = harvestRepository;
        this.waterSourceRepository = waterSourceRepository;
    }

    public List<RainwaterHarvest> findAll() {
        return harvestRepository.findAll();
    }

    @Transactional
    public RainwaterHarvest create(RainwaterHarvest harvest) {
        validate(harvest);
        if (harvest.getHarvestedLiters() == null) {
            harvest.setHarvestedLiters(calculate(harvest));
        }
        return harvestRepository.save(harvest);
    }

    public RainwaterHarvest get(UUID harvestId) {
        return harvestRepository.findById(harvestId)
                .orElseThrow(() -> new NotFoundException("RainwaterHarvest"));
    }

    @Transactional
    public RainwaterHarvest update(UUID harvestId, RainwaterHarvest payload) {
        RainwaterHarvest harvest = get(harvestId);
        harvest.setSourceId(payload.getSourceId() == null ? harvest.getSourceId() : payload.getSourceId());
        harvest.setCatchmentAreaM2(payload.getCatchmentAreaM2() == null ? harvest.getCatchmentAreaM2() : payload.getCatchmentAreaM2());
        harvest.setRainfallMm(payload.getRainfallMm() == null ? harvest.getRainfallMm() : payload.getRainfallMm());
        harvest.setRunoffCoefficient(payload.getRunoffCoefficient() == null ? harvest.getRunoffCoefficient() : payload.getRunoffCoefficient());
        harvest.setHarvestedLiters(payload.getHarvestedLiters() == null ? calculate(harvest) : payload.getHarvestedLiters());
        harvest.setCaptureDate(payload.getCaptureDate() == null ? harvest.getCaptureDate() : payload.getCaptureDate());
        validate(harvest);
        return harvestRepository.save(harvest);
    }

    @Transactional
    public void delete(UUID harvestId) {
        harvestRepository.delete(get(harvestId));
    }

    public Map<String, Object> coverage(String period) {
        double totalHarvested = harvestRepository.findAll().stream()
                .filter(harvest -> isInPeriod(harvest.getCaptureDate(), period))
                .mapToDouble(harvest -> harvest.getHarvestedLiters() == null ? 0 : harvest.getHarvestedLiters())
                .sum();
        return Map.of("period", period, "rainwater_harvested_liters", totalHarvested);
    }

    private void validate(RainwaterHarvest harvest) {
        requireSource(harvest.getSourceId());
        if (harvest.getCatchmentAreaM2() == null || harvest.getCatchmentAreaM2() < 0
                || harvest.getRainfallMm() == null || harvest.getRainfallMm() < 0
                || harvest.getRunoffCoefficient() == null
                || harvest.getRunoffCoefficient() < 0 || harvest.getRunoffCoefficient() > 1) {
            throw new IllegalArgumentException("Rainwater measurements must be non-negative and runoff coefficient must be between 0 and 1");
        }
    }

    private void requireSource(UUID sourceId) {
        if (sourceId == null || waterSourceRepository.findById(sourceId).isEmpty()) {
            throw new NotFoundException("WaterSource");
        }
    }

    private double calculate(RainwaterHarvest harvest) {
        return harvest.getCatchmentAreaM2() * harvest.getRainfallMm() * harvest.getRunoffCoefficient();
    }

    private boolean isInPeriod(Instant instant, String period) {
        if (instant == null) {
            return false;
        }
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate date = instant.atZone(ZoneOffset.UTC).toLocalDate();
        if ("week".equalsIgnoreCase(period)) {
            return !date.isBefore(today.minus(7, ChronoUnit.DAYS));
        }
        if ("year".equalsIgnoreCase(period)) {
            return !date.isBefore(today.minus(1, ChronoUnit.YEARS));
        }
        return !date.isBefore(today.minus(1, ChronoUnit.MONTHS));
    }
}