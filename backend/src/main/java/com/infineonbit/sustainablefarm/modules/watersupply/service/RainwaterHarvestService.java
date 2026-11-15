package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.RainwaterHarvest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.PageResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.RainwaterHarvestRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.RainwaterHarvestResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public List<RainwaterHarvestResponse> findAll() {
        return harvestRepository.findAll().stream().map(RainwaterHarvestResponse::from).toList();
    }

    /** Paginated list, with optional source filter: same contract as the other module lists. */
    public PageResponse<RainwaterHarvestResponse> findAll(Pageable pageable, UUID sourceId) {
        Page<RainwaterHarvest> page = sourceId == null
                ? harvestRepository.findAll(pageable)
                : harvestRepository.findBySourceId(sourceId, pageable);
        return new PageResponse<>(page.getContent().stream().map(RainwaterHarvestResponse::from).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public RainwaterHarvestResponse create(RainwaterHarvestRequest request) {
        RainwaterHarvest harvest = new RainwaterHarvest();
        harvest.setSourceId(request.sourceId());
        harvest.setCatchmentAreaM2(request.catchmentAreaM2());
        harvest.setRainfallMm(request.rainfallMm());
        harvest.setRunoffCoefficient(request.runoffCoefficient());
        harvest.setHarvestedLiters(request.harvestedLiters());
        harvest.setCaptureDate(request.captureDate());
        validate(harvest);
        if (harvest.getHarvestedLiters() == null) {
            harvest.setHarvestedLiters(calculate(harvest));
        }
        return RainwaterHarvestResponse.from(harvestRepository.save(harvest));
    }

    public RainwaterHarvestResponse get(UUID harvestId) {
        return RainwaterHarvestResponse.from(getEntity(harvestId));
    }

    @Transactional
    public RainwaterHarvestResponse update(UUID harvestId, RainwaterHarvestRequest request) {
        RainwaterHarvest harvest = getEntity(harvestId);
        harvest.setSourceId(request.sourceId() == null ? harvest.getSourceId() : request.sourceId());
        harvest.setCatchmentAreaM2(request.catchmentAreaM2() == null ? harvest.getCatchmentAreaM2() : request.catchmentAreaM2());
        harvest.setRainfallMm(request.rainfallMm() == null ? harvest.getRainfallMm() : request.rainfallMm());
        harvest.setRunoffCoefficient(request.runoffCoefficient() == null ? harvest.getRunoffCoefficient() : request.runoffCoefficient());
        harvest.setHarvestedLiters(request.harvestedLiters() == null ? calculate(harvest) : request.harvestedLiters());
        harvest.setCaptureDate(request.captureDate() == null ? harvest.getCaptureDate() : request.captureDate());
        validate(harvest);
        return RainwaterHarvestResponse.from(harvestRepository.save(harvest));
    }

    @Transactional
    public void delete(UUID harvestId) {
        harvestRepository.delete(getEntity(harvestId));
    }

    private RainwaterHarvest getEntity(UUID harvestId) {
        return harvestRepository.findById(harvestId)
                .orElseThrow(() -> new NotFoundException("RainwaterHarvest"));
    }

    /** Volume harvested over the requested period, computed by the database (no full scan application-side). */
    public Map<String, Object> coverage(String period) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant start = periodStart(period, today).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        double totalHarvested = harvestRepository.sumHarvestedLitersBetween(start, end);
        return Map.of("period", period, "rainwater_harvested_liters", totalHarvested);
    }

    private LocalDate periodStart(String period, LocalDate today) {
        if ("week".equalsIgnoreCase(period)) {
            return today.minus(7, ChronoUnit.DAYS);
        }
        if ("year".equalsIgnoreCase(period)) {
            return today.minus(1, ChronoUnit.YEARS);
        }
        return today.minus(1, ChronoUnit.MONTHS);
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
}