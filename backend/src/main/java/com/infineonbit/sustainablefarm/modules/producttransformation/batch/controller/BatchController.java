package com.infineonbit.sustainablefarm.modules.producttransformation.batch.controller;

import com.infineonbit.sustainablefarm.modules.producttransformation.dashboard.service.DtoMapper;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.dto.request.BatchCreateRequest;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.dto.request.BatchStatusAdvanceRequest;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.dto.request.BatchUpdateRequest;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.dto.response.BatchResponse;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.model.Batch;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.model.Batch.BatchStatus;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.service.BatchService;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent.MangoVariety;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.repository.HarvestEventRepository;
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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * REST Controller for Batch operations
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/batches")
@Tag(name = "Batch Management", description = "APIs for managing mango processing batches")
public class BatchController {

    private final BatchService batchService;
    private final DtoMapper dtoMapper;
    private final HarvestEventRepository harvestEventRepository;

    @Autowired
    public BatchController(BatchService batchService, DtoMapper dtoMapper,
                           HarvestEventRepository harvestEventRepository) {
        this.batchService = batchService;
        this.dtoMapper = dtoMapper;
        this.harvestEventRepository = harvestEventRepository;
    }

    /**
     * Resolves the Plants identity (lot number, plants harvest id) of the
     * harvest event linked to this batch, joined by batch_id (option B).
     * Returns null when the batch does not come from the Plants import.
     */
    private HarvestEvent harvestEventFor(String batchId) {
        if (batchId == null) {
            return null;
        }
        return harvestEventRepository.findByBatchId(batchId).orElse(null);
    }

    /**
     * Batched resolution of the Plants identity for many batches, avoiding
     * one query per row on list endpoints.
     */
    private Map<String, HarvestEvent> harvestEventsFor(List<Batch> batches) {
        if (batches == null || batches.isEmpty()) {
            return Map.of();
        }
        List<String> batchIds = batches.stream().map(Batch::getBatchId).collect(Collectors.toList());
        return harvestEventRepository.findByBatchIdIn(batchIds).stream()
                .collect(Collectors.toMap(HarvestEvent::getBatchId, Function.identity(), (first, second) -> first));
    }

    private List<BatchResponse> toResponses(List<Batch> batches) {
        Map<String, HarvestEvent> eventsByBatchId = harvestEventsFor(batches);
        return batches.stream()
                .map(b -> dtoMapper.toResponse(b, eventsByBatchId.get(b.getBatchId())))
                .collect(Collectors.toList());
    }

    @PostMapping
    @Operation(summary = "Create a new batch", description = "Creates a new batch with the provided details")
    public ResponseEntity<BatchResponse> createBatch(@Valid @RequestBody BatchCreateRequest request) {
        Batch batch = dtoMapper.toEntity(request);
        Batch createdBatch = batchService.createBatch(batch);
        BatchResponse response = dtoMapper.toResponse(createdBatch, harvestEventFor(createdBatch.getBatchId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{batchId}")
    @Operation(summary = "Get batch by ID", description = "Retrieves a specific batch by its ID")
    public ResponseEntity<BatchResponse> getBatchById(
            @Parameter(description = "Batch ID") @PathVariable String batchId) {
        Batch batch = batchService.getBatchById(batchId);
        BatchResponse response = dtoMapper.toResponse(batch, harvestEventFor(batch.getBatchId()));
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all batches", description = "Retrieves all batches")
    public ResponseEntity<List<BatchResponse>> getAllBatches() {
        List<Batch> batches = batchService.getAllBatches();
        return ResponseEntity.ok(toResponses(batches));
    }

    @PutMapping("/{batchId}")
    @Operation(summary = "Update batch", description = "Updates an existing batch")
    public ResponseEntity<BatchResponse> updateBatch(
            @Parameter(description = "Batch ID") @PathVariable String batchId,
            @Valid @RequestBody BatchUpdateRequest request) {
        Batch batch = new Batch();
        batch.setHarvestDate(request.getHarvestDate());
        batch.setMangoVariety(request.getMangoVariety());
        batch.setHarvestQuantityKg(request.getHarvestQuantityKg());
        batch.setFarmId(request.getFarmId());
        batch.setBlockId(request.getBlockId());
        batch.setCurrentStatus(request.getCurrentStatus());
        
        Batch updatedBatch = batchService.updateBatch(batchId, batch);
        BatchResponse response = dtoMapper.toResponse(updatedBatch, harvestEventFor(updatedBatch.getBatchId()));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{batchId}")
    @Operation(summary = "Delete batch", description = "Deletes a batch by its ID")
    public ResponseEntity<Void> deleteBatch(
            @Parameter(description = "Batch ID") @PathVariable String batchId) {
        batchService.deleteBatch(batchId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{batchId}/advance-status")
    @Operation(summary = "Advance batch status", description = "Advances batch status to next stage following business rules")
    public ResponseEntity<BatchResponse> advanceBatchStatus(
            @Parameter(description = "Batch ID") @PathVariable String batchId,
            @Valid @RequestBody(required = false) BatchStatusAdvanceRequest request) {
        Batch batch = batchService.advanceBatchStatus(batchId);
        BatchResponse response = dtoMapper.toResponse(batch, harvestEventFor(batch.getBatchId()));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{batchId}/status")
    @Operation(summary = "Set batch status", description = "Manually sets batch status (for operations like REJECTED)")
    public ResponseEntity<BatchResponse> setBatchStatus(
            @Parameter(description = "Batch ID") @PathVariable String batchId,
            @Parameter(description = "New status") @RequestParam BatchStatus status) {
        Batch batch = batchService.setBatchStatus(batchId, status);
        BatchResponse response = dtoMapper.toResponse(batch, harvestEventFor(batch.getBatchId()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get batches by status", description = "Retrieves batches filtered by status")
    public ResponseEntity<List<BatchResponse>> getBatchesByStatus(
            @Parameter(description = "Batch status") @PathVariable BatchStatus status) {
        List<Batch> batches = batchService.getBatchesByStatus(status);
        return ResponseEntity.ok(toResponses(batches));
    }

    @GetMapping("/farm/{farmId}")
    @Operation(summary = "Get batches by farm", description = "Retrieves batches filtered by farm ID")
    public ResponseEntity<List<BatchResponse>> getBatchesByFarm(
            @Parameter(description = "Farm ID") @PathVariable String farmId) {
        List<Batch> batches = batchService.getBatchesByFarm(farmId);
        return ResponseEntity.ok(toResponses(batches));
    }

    @GetMapping("/variety/{variety}")
    @Operation(summary = "Get batches by variety", description = "Retrieves batches filtered by mango variety")
    public ResponseEntity<List<BatchResponse>> getBatchesByVariety(
            @Parameter(description = "Mango variety") @PathVariable MangoVariety variety) {
        List<Batch> batches = batchService.getBatchesByVariety(variety);
        return ResponseEntity.ok(toResponses(batches));
    }

    @GetMapping("/ready-for-shipping")
    @Operation(summary = "Get batches ready for shipping", description = "Retrieves batches that are ready for shipping")
    public ResponseEntity<List<BatchResponse>> getBatchesReadyForShipping() {
        List<Batch> batches = batchService.getBatchesReadyForShipping();
        return ResponseEntity.ok(toResponses(batches));
    }

    @GetMapping("/farm/{farmId}/block/{blockId}")
    @Operation(summary = "Get batches by farm and block", description = "Retrieves batches filtered by farm and block ID")
    public ResponseEntity<List<BatchResponse>> getBatchesByFarmAndBlock(
            @Parameter(description = "Farm ID") @PathVariable String farmId,
            @Parameter(description = "Block ID") @PathVariable String blockId) {
        List<Batch> batches = batchService.getBatchesByFarmAndBlock(farmId, blockId);
        return ResponseEntity.ok(toResponses(batches));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get batches by harvest date range", description = "Retrieves batches within a harvest date range")
    public ResponseEntity<List<BatchResponse>> getBatchesByHarvestDateRange(
            @Parameter(description = "Start date") @RequestParam LocalDate startDate,
            @Parameter(description = "End date") @RequestParam LocalDate endDate) {
        List<Batch> batches = batchService.getBatchesByHarvestDateRange(startDate, endDate);
        return ResponseEntity.ok(toResponses(batches));
    }
}
