package com.sustainablefarm.controller;

import com.sustainablefarm.core.dto.mapper.DtoMapper;
import com.sustainablefarm.dto.request.HistoricalHarvestCreateRequest;
import com.sustainablefarm.dto.response.HistoricalHarvestResponse;
import com.sustainablefarm.model.HarvestEvent.MangoVariety;
import com.sustainablefarm.model.HistoricalHarvest;
import com.sustainablefarm.service.HistoricalHarvestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for HistoricalHarvest operations (for forecasting)
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/historical-harvests")
@Tag(name = "Historical Harvest Management", description = "APIs for managing historical harvest data for forecasting")
public class HistoricalHarvestController {

    private final HistoricalHarvestService historicalHarvestService;
    private final DtoMapper dtoMapper;

    @Autowired
    public HistoricalHarvestController(HistoricalHarvestService historicalHarvestService, DtoMapper dtoMapper) {
        this.historicalHarvestService = historicalHarvestService;
        this.dtoMapper = dtoMapper;
    }

    @PostMapping
    @Operation(summary = "Create historical harvest record", description = "Creates a new historical harvest record for forecasting")
    public ResponseEntity<HistoricalHarvestResponse> createHistoricalHarvest(@Valid @RequestBody HistoricalHarvestCreateRequest request) {
        HistoricalHarvest historical = dtoMapper.toEntity(request);
        HistoricalHarvest createdHistorical = historicalHarvestService.createHistoricalHarvest(historical);
        HistoricalHarvestResponse response = dtoMapper.toResponse(createdHistorical);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all historical harvest records", description = "Retrieves all historical harvest records")
    public ResponseEntity<List<HistoricalHarvestResponse>> getAllHistoricalHarvests() {
        List<HistoricalHarvest> historicalList = historicalHarvestService.getAllHistoricalHarvests();
        List<HistoricalHarvestResponse> responses = historicalList.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/year/{year}")
    @Operation(summary = "Get historical harvests by year", description = "Retrieves historical harvest records filtered by year")
    public ResponseEntity<List<HistoricalHarvestResponse>> getHistoricalHarvestsByYear(
            @Parameter(description = "Year") @PathVariable Integer year) {
        List<HistoricalHarvest> historicalList = historicalHarvestService.getHistoricalHarvestByYear(year);
        List<HistoricalHarvestResponse> responses = historicalList.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/variety/{variety}")
    @Operation(summary = "Get historical harvests by variety", description = "Retrieves historical harvest records filtered by mango variety")
    public ResponseEntity<List<HistoricalHarvestResponse>> getHistoricalHarvestsByVariety(
            @Parameter(description = "Mango variety") @PathVariable MangoVariety variety) {
        List<HistoricalHarvest> historicalList = historicalHarvestService.getHistoricalHarvestByVariety(variety);
        List<HistoricalHarvestResponse> responses = historicalList.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/year/{year}/month/{month}")
    @Operation(summary = "Get historical harvests by year and month", description = "Retrieves historical harvest records filtered by year and month")
    public ResponseEntity<List<HistoricalHarvestResponse>> getHistoricalHarvestsByYearAndMonth(
            @Parameter(description = "Year") @PathVariable Integer year,
            @Parameter(description = "Month") @PathVariable Integer month) {
        List<HistoricalHarvest> historicalList = historicalHarvestService.getHistoricalHarvestByYearAndMonth(year, month);
        List<HistoricalHarvestResponse> responses = historicalList.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/variety/{variety}/year/{year}")
    @Operation(summary = "Get historical harvests by variety and year", description = "Retrieves historical harvest records filtered by variety and year")
    public ResponseEntity<List<HistoricalHarvestResponse>> getHistoricalHarvestsByVarietyAndYear(
            @Parameter(description = "Mango variety") @PathVariable MangoVariety variety,
            @Parameter(description = "Year") @PathVariable Integer year) {
        List<HistoricalHarvest> historicalList = historicalHarvestService.getHistoricalHarvestByYearAndVariety(year, variety);
        List<HistoricalHarvestResponse> responses = historicalList.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/year/{year}/total-quantity")
    @Operation(summary = "Get total harvest quantity by year", description = "Calculates total harvest quantity for a specific year")
    public ResponseEntity<Double> getTotalHarvestQuantityByYear(
            @Parameter(description = "Year") @PathVariable Integer year) {
        Double totalQuantity = historicalHarvestService.getTotalHarvestByYear(year);
        return ResponseEntity.ok(totalQuantity);
    }

    @DeleteMapping("/year/{year}/month/{month}/week/{week}/variety/{variety}")
    @Operation(summary = "Delete historical harvest record", description = "Deletes a historical harvest record by composite key")
    public ResponseEntity<Void> deleteHistoricalHarvest(
            @Parameter(description = "Year") @PathVariable Integer year,
            @Parameter(description = "Month") @PathVariable Integer month,
            @Parameter(description = "Week") @PathVariable Integer week,
            @Parameter(description = "Mango variety") @PathVariable MangoVariety variety) {
        historicalHarvestService.deleteHistoricalHarvest(year, month, week, variety);
        return ResponseEntity.noContent().build();
    }
}
