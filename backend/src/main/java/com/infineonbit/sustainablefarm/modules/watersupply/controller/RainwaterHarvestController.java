package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.RainwaterHarvest;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.RainwaterHarvestRepository;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rainwater-harvests")
public class RainwaterHarvestController {
    private final RainwaterHarvestRepository rainwaterHarvestRepository;

    public RainwaterHarvestController(RainwaterHarvestRepository rainwaterHarvestRepository) {
        this.rainwaterHarvestRepository = rainwaterHarvestRepository;
    }

    @GetMapping
    public List<RainwaterHarvest> list() {
        return rainwaterHarvestRepository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RainwaterHarvest create(@Valid @RequestBody RainwaterHarvest harvest) {
        return rainwaterHarvestRepository.save(harvest);
    }

    @GetMapping("/{harvestId}")
    public RainwaterHarvest get(@PathVariable UUID harvestId) {
        return rainwaterHarvestRepository.findById(harvestId)
                .orElseThrow(() -> new NotFoundException("RainwaterHarvest"));
    }

    @PutMapping("/{harvestId}")
    public RainwaterHarvest update(@PathVariable UUID harvestId, @RequestBody RainwaterHarvest payload) {
        RainwaterHarvest harvest = rainwaterHarvestRepository.findById(harvestId)
                .orElseThrow(() -> new NotFoundException("RainwaterHarvest"));
        harvest.setSourceId(payload.getSourceId() == null ? harvest.getSourceId() : payload.getSourceId());
        harvest.setCatchmentAreaM2(payload.getCatchmentAreaM2() == null ? harvest.getCatchmentAreaM2() : payload.getCatchmentAreaM2());
        harvest.setRainfallMm(payload.getRainfallMm() == null ? harvest.getRainfallMm() : payload.getRainfallMm());
        harvest.setRunoffCoefficient(payload.getRunoffCoefficient() == null ? harvest.getRunoffCoefficient() : payload.getRunoffCoefficient());
        if (payload.getHarvestedLiters() == null && payload.getCatchmentAreaM2() != null
                && payload.getRainfallMm() != null && payload.getRunoffCoefficient() != null) {
            harvest.setHarvestedLiters(payload.getCatchmentAreaM2() * payload.getRainfallMm() * payload.getRunoffCoefficient());
        } else {
            harvest.setHarvestedLiters(payload.getHarvestedLiters() == null ? harvest.getHarvestedLiters() : payload.getHarvestedLiters());
        }
        harvest.setCaptureDate(payload.getCaptureDate() == null ? harvest.getCaptureDate() : payload.getCaptureDate());
        return rainwaterHarvestRepository.save(harvest);
    }

    @DeleteMapping("/{harvestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID harvestId) {
        rainwaterHarvestRepository.delete(rainwaterHarvestRepository.findById(harvestId)
                .orElseThrow(() -> new NotFoundException("RainwaterHarvest")));
    }

    @GetMapping("/coverage")
    public Map<String, Object> coverage(@RequestParam(defaultValue = "month") String period) {
        List<RainwaterHarvest> harvests = rainwaterHarvestRepository.findAll();
        double totalHarvested = 0;
        for (RainwaterHarvest harvest : harvests) {
            Instant captureDate = harvest.getCaptureDate();
            if (isInPeriod(captureDate, period)) {
                totalHarvested += harvest.getHarvestedLiters() == null ? 0 : harvest.getHarvestedLiters();
            }
        }
        return Map.of("period", period, "rainwater_harvested_liters", totalHarvested);
    }

    private boolean isInPeriod(Instant instant, String period) {
        if (instant == null) {
            return false;
        }
        ZoneOffset utc = ZoneOffset.UTC;
        LocalDate today = LocalDate.now(utc);
        LocalDate date = instant.atZone(utc).toLocalDate();
        if ("week".equalsIgnoreCase(period)) {
            return !date.isBefore(today.minus(7, ChronoUnit.DAYS));
        }
        if ("year".equalsIgnoreCase(period)) {
            return !date.isBefore(today.minus(1, ChronoUnit.YEARS));
        }
        // default: month
        return !date.isBefore(today.minus(1, ChronoUnit.MONTHS));
    }
}
