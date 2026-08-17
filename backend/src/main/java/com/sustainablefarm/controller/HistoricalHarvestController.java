package com.sustainablefarm.controller;

import com.sustainablefarm.dto.mapper.DtoMapper;
import com.sustainablefarm.dto.request.HistoricalHarvestCreateRequest;
import com.sustainablefarm.dto.request.HistoricalHarvestUpdateRequest;
import com.sustainablefarm.dto.response.HistoricalHarvestResponse;
import com.sustainablefarm.dto.response.PageResponse;
import com.sustainablefarm.model.HistoricalHarvest;
import com.sustainablefarm.model.HarvestEvent.MangoVariety;
import com.sustainablefarm.service.HistoricalHarvestService;
import com.sustainablefarm.util.PaginationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/historical-harvests")
@Tag(name = "Historical Harvest", description = "APIs for aggregated historical harvest data and forecasting")
public class HistoricalHarvestController {

    private final HistoricalHarvestService historicalHarvestService;
    private final DtoMapper dtoMapper;

    @Autowired
    public HistoricalHarvestController(HistoricalHarvestService historicalHarvestService, DtoMapper dtoMapper) {
        this.historicalHarvestService = historicalHarvestService;
        this.dtoMapper = dtoMapper;
    }

    @PostMapping
    @Operation(summary = "Create historical harvest record")
    public ResponseEntity<HistoricalHarvestResponse> createHistoricalHarvest(
            @Valid @RequestBody HistoricalHarvestCreateRequest request) {
        HistoricalHarvest historical = dtoMapper.toEntity(request);
        HistoricalHarvest created = historicalHarvestService.createHistoricalHarvest(historical);
        return ResponseEntity.status(HttpStatus.CREATED).body(dtoMapper.toResponse(created));
    }

    @GetMapping("/{year}/{month}/{week}/{variety}")
    @Operation(summary = "Get historical harvest by composite key")
    public ResponseEntity<HistoricalHarvestResponse> getHistoricalHarvestById(
            @PathVariable Integer year,
            @PathVariable Integer month,
            @PathVariable Integer week,
            @PathVariable MangoVariety variety) {
        HistoricalHarvest historical = historicalHarvestService.getHistoricalHarvestById(year, month, week, variety);
        return ResponseEntity.ok(dtoMapper.toResponse(historical));
    }

    @GetMapping
    @Operation(summary = "Get all historical harvest records (paginated)")
    public ResponseEntity<PageResponse<HistoricalHarvestResponse>> getAllHistoricalHarvests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<HistoricalHarvestResponse> responses = historicalHarvestService.getAllHistoricalHarvests().stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(PaginationUtils.paginate(responses, page, size));
    }

    @PutMapping("/{year}/{month}/{week}/{variety}")
    @Operation(summary = "Update historical harvest record")
    public ResponseEntity<HistoricalHarvestResponse> updateHistoricalHarvest(
            @PathVariable Integer year,
            @PathVariable Integer month,
            @PathVariable Integer week,
            @PathVariable MangoVariety variety,
            @Valid @RequestBody HistoricalHarvestUpdateRequest request) {
        HistoricalHarvest existing = historicalHarvestService.getHistoricalHarvestById(year, month, week, variety);
        applyUpdate(existing, request);
        HistoricalHarvest updated = historicalHarvestService.updateHistoricalHarvest(
                year, month, week, variety, existing);
        return ResponseEntity.ok(dtoMapper.toResponse(updated));
    }

    @DeleteMapping("/{year}/{month}/{week}/{variety}")
    @Operation(summary = "Delete historical harvest record")
    public ResponseEntity<Void> deleteHistoricalHarvest(
            @PathVariable Integer year,
            @PathVariable Integer month,
            @PathVariable Integer week,
            @PathVariable MangoVariety variety) {
        historicalHarvestService.deleteHistoricalHarvest(year, month, week, variety);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{year}/{month}/{week}/{variety}/aggregate")
    @Operation(summary = "Aggregate historical harvest from harvest events")
    public ResponseEntity<HistoricalHarvestResponse> aggregateFromHarvestEvents(
            @PathVariable Integer year,
            @PathVariable Integer month,
            @PathVariable Integer week,
            @PathVariable MangoVariety variety) {
        HistoricalHarvest aggregated = historicalHarvestService.aggregateFromHarvestEvents(
                year, month, week, variety);
        return ResponseEntity.ok(dtoMapper.toResponse(aggregated));
    }

    @GetMapping("/year/{year}")
    @Operation(summary = "Get historical harvest records by year")
    public ResponseEntity<List<HistoricalHarvestResponse>> getHistoricalHarvestByYear(@PathVariable Integer year) {
        List<HistoricalHarvestResponse> responses = historicalHarvestService.getHistoricalHarvestByYear(year).stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/year/{year}/month/{month}")
    @Operation(summary = "Get historical harvest records by year and month")
    public ResponseEntity<List<HistoricalHarvestResponse>> getHistoricalHarvestByYearAndMonth(
            @PathVariable Integer year,
            @PathVariable Integer month) {
        List<HistoricalHarvestResponse> responses = historicalHarvestService
                .getHistoricalHarvestByYearAndMonth(year, month).stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/variety/{variety}")
    @Operation(summary = "Get historical harvest records by variety")
    public ResponseEntity<List<HistoricalHarvestResponse>> getHistoricalHarvestByVariety(
            @PathVariable MangoVariety variety) {
        List<HistoricalHarvestResponse> responses = historicalHarvestService.getHistoricalHarvestByVariety(variety).stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/forecasting")
    @Operation(summary = "Get historical data for forecasting")
    public ResponseEntity<List<HistoricalHarvestResponse>> getForecastingData(
            @RequestParam(defaultValue = "2018") Integer minYear) {
        List<HistoricalHarvestResponse> responses = historicalHarvestService.getForecastingData(minYear).stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    private void applyUpdate(HistoricalHarvest existing, HistoricalHarvestUpdateRequest request) {
        if (request.getHarvestQuantityKg() != null) {
            existing.setHarvestQuantityKg(request.getHarvestQuantityKg());
        }
        if (request.getQualityGradeAPct() != null) {
            existing.setQualityGradeAPct(request.getQualityGradeAPct());
        }
        if (request.getQualityGradeBPct() != null) {
            existing.setQualityGradeBPct(request.getQualityGradeBPct());
        }
        if (request.getQualityGradeCPct() != null) {
            existing.setQualityGradeCPct(request.getQualityGradeCPct());
        }
        if (request.getWeatherCondition() != null) {
            existing.setWeatherCondition(request.getWeatherCondition());
        }
        if (request.getRainfallMm() != null) {
            existing.setRainfallMm(request.getRainfallMm());
        }
        if (request.getTemperatureAvgC() != null) {
            existing.setTemperatureAvgC(request.getTemperatureAvgC());
        }
    }
}
