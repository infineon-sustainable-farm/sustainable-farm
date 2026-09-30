package com.infineonbit.sustainablefarm.modules.producttransformation.resources.harvestevent.controller;

import com.infineonbit.sustainablefarm.modules.producttransformation.resources.harvestevent.model.HarvestEvent;
import com.infineonbit.sustainablefarm.modules.producttransformation.resources.harvestevent.service.HarvestEventService;
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

    public HarvestEventController(HarvestEventService harvestEventService) {
        this.harvestEventService = harvestEventService;
    }

    @PostMapping
    @Operation(summary = "Create a new harvest event", description = "Creates a new harvest event (from Plants workstream)")
    public ResponseEntity<HarvestEvent> createHarvestEvent(@Valid @RequestBody HarvestEvent request) {
        HarvestEvent createdEvent = harvestEventService.createHarvestEvent(request);
        return new ResponseEntity<>(createdEvent, HttpStatus.CREATED);
    }

    @GetMapping("/{harvestId}")
    @Operation(summary = "Get harvest event by ID", description = "Retrieves a specific harvest event by its ID")
    public ResponseEntity<HarvestEvent> getHarvestEventById(
            @Parameter(description = "Harvest ID") @PathVariable String harvestId) {
        HarvestEvent event = harvestEventService.getHarvestEventById(harvestId);
        return ResponseEntity.ok(event);
    }

    @GetMapping("/batch/{batchId}")
    @Operation(summary = "Get harvest event by batch ID", description = "Retrieves harvest event for a specific batch")
    public ResponseEntity<HarvestEvent> getHarvestEventByBatchId(
            @Parameter(description = "Batch ID") @PathVariable String batchId) {
        HarvestEvent event = harvestEventService.getHarvestEventByBatchId(batchId);
        return ResponseEntity.ok(event);
    }

    @GetMapping
    @Operation(summary = "Get all harvest events", description = "Retrieves all harvest event records")
    public ResponseEntity<List<HarvestEvent>> getAllHarvestEvents() {
        List<HarvestEvent> events = harvestEventService.getAllHarvestEvents();
        return ResponseEntity.ok(events);
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
    public ResponseEntity<List<HarvestEvent>> getHarvestEventsByDateRange(
            @Parameter(description = "Start date") @RequestParam LocalDate startDate,
            @Parameter(description = "End date") @RequestParam LocalDate endDate) {
        List<HarvestEvent> events = harvestEventService.getHarvestEventsByDateRange(startDate, endDate);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/variety/{variety}")
    @Operation(summary = "Get harvest events by variety", description = "Retrieves harvest events filtered by mango variety")
    public ResponseEntity<List<HarvestEvent>> getHarvestEventsByVariety(
            @Parameter(description = "Mango variety") @PathVariable String variety) {
        HarvestEvent.MangoVariety varietyEnum = HarvestEvent.MangoVariety.valueOf(variety.toUpperCase());
        List<HarvestEvent> events = harvestEventService.getHarvestEventsByVariety(varietyEnum);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/grade/{grade}")
    @Operation(summary = "Get harvest events by grade", description = "Retrieves harvest events filtered by quality grade")
    public ResponseEntity<List<HarvestEvent>> getHarvestEventsByGrade(
            @Parameter(description = "Quality grade") @PathVariable String grade) {
        HarvestEvent.QualityGrade gradeEnum = HarvestEvent.QualityGrade.valueOf(grade.toUpperCase());
        List<HarvestEvent> events = harvestEventService.getHarvestEventsByQualityGrade(gradeEnum);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/farm/{farmId}")
    @Operation(summary = "Get harvest events by farm", description = "Retrieves harvest events filtered by farm ID")
    public ResponseEntity<List<HarvestEvent>> getHarvestEventsByFarm(
            @Parameter(description = "Farm ID") @PathVariable String farmId) {
        List<HarvestEvent> events = harvestEventService.getHarvestEventsByFarm(farmId);
        return ResponseEntity.ok(events);
    }
}