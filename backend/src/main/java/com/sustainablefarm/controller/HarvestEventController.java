package com.sustainablefarm.controller;

import com.sustainablefarm.dto.mapper.DtoMapper;
import com.sustainablefarm.dto.request.HarvestEventCreateRequest;
import com.sustainablefarm.dto.response.HarvestEventResponse;
import com.sustainablefarm.model.HarvestEvent;
import com.sustainablefarm.service.HarvestEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for HarvestEvent operations (from Plants workstream)
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/harvest-events")
@Tag(name = "Harvest Event Management", description = "APIs for managing harvest events from Plants workstream")
public class HarvestEventController {

    private final HarvestEventService harvestEventService;
    private final DtoMapper dtoMapper;

    @Autowired
    public HarvestEventController(HarvestEventService harvestEventService, DtoMapper dtoMapper) {
        this.harvestEventService = harvestEventService;
        this.dtoMapper = dtoMapper;
    }

    @PostMapping
    @Operation(summary = "Create a new harvest event", description = "Creates a new harvest event (from Plants workstream)")
    public ResponseEntity<HarvestEventResponse> createHarvestEvent(@Valid @RequestBody HarvestEventCreateRequest request) {
        HarvestEvent event = dtoMapper.toEntity(request);
        HarvestEvent createdEvent = harvestEventService.createHarvestEvent(event);
        HarvestEventResponse response = dtoMapper.toResponse(createdEvent);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{harvestId}")
    @Operation(summary = "Get harvest event by ID", description = "Retrieves a specific harvest event by its ID")
    public ResponseEntity<HarvestEventResponse> getHarvestEventById(
            @Parameter(description = "Harvest ID") @PathVariable String harvestId) {
        HarvestEvent event = harvestEventService.getHarvestEventById(harvestId);
        HarvestEventResponse response = dtoMapper.toResponse(event);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/batch/{batchId}")
    @Operation(summary = "Get harvest event by batch ID", description = "Retrieves harvest event for a specific batch")
    public ResponseEntity<HarvestEventResponse> getHarvestEventByBatchId(
            @Parameter(description = "Batch ID") @PathVariable String batchId) {
        HarvestEvent event = harvestEventService.getHarvestEventByBatchId(batchId);
        HarvestEventResponse response = dtoMapper.toResponse(event);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all harvest events", description = "Retrieves all harvest event records")
    public ResponseEntity<List<HarvestEventResponse>> getAllHarvestEvents() {
        List<HarvestEvent> events = harvestEventService.getAllHarvestEvents();
        List<HarvestEventResponse> responses = events.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{harvestId}")
    @Operation(summary = "Delete harvest event", description = "Deletes a harvest event by its ID")
    public ResponseEntity<Void> deleteHarvestEvent(
            @Parameter(description = "Harvest ID") @PathVariable String harvestId) {
        harvestEventService.deleteHarvestEvent(harvestId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get harvest events by date range", description = "Retrieves harvest events within a date range")
    public ResponseEntity<List<HarvestEventResponse>> getHarvestEventsByDateRange(
            @Parameter(description = "Start date") @RequestParam LocalDate startDate,
            @Parameter(description = "End date") @RequestParam LocalDate endDate) {
        List<HarvestEvent> events = harvestEventService.getHarvestEventsByDateRange(startDate, endDate);
        List<HarvestEventResponse> responses = events.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/variety/{variety}")
    @Operation(summary = "Get harvest events by variety", description = "Retrieves harvest events filtered by mango variety")
    public ResponseEntity<List<HarvestEventResponse>> getHarvestEventsByVariety(
            @Parameter(description = "Mango variety") @PathVariable com.sustainablefarm.model.HarvestEvent.MangoVariety variety) {
        List<HarvestEvent> events = harvestEventService.getHarvestEventsByVariety(variety);
        List<HarvestEventResponse> responses = events.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/grade/{grade}")
    @Operation(summary = "Get harvest events by grade", description = "Retrieves harvest events filtered by quality grade")
    public ResponseEntity<List<HarvestEventResponse>> getHarvestEventsByGrade(
            @Parameter(description = "Quality grade") @PathVariable com.sustainablefarm.model.HarvestEvent.QualityGrade grade) {
        List<HarvestEvent> events = harvestEventService.getHarvestEventsByQualityGrade(grade);
        List<HarvestEventResponse> responses = events.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/farm/{farmId}")
    @Operation(summary = "Get harvest events by farm", description = "Retrieves harvest events filtered by farm ID")
    public ResponseEntity<List<HarvestEventResponse>> getHarvestEventsByFarm(
            @Parameter(description = "Farm ID") @PathVariable String farmId) {
        List<HarvestEvent> events = harvestEventService.getHarvestEventsByFarm(farmId);
        List<HarvestEventResponse> responses = events.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}
