package com.sustainablefarm.controller;

import com.sustainablefarm.core.dto.mapper.DtoMapper;
import com.sustainablefarm.dto.request.BatchCreateRequest;
import com.sustainablefarm.dto.request.BatchStatusAdvanceRequest;
import com.sustainablefarm.dto.request.BatchUpdateRequest;
import com.sustainablefarm.dto.response.BatchResponse;
import com.sustainablefarm.model.Batch;
import com.sustainablefarm.model.Batch.BatchStatus;
import com.sustainablefarm.model.HarvestEvent.MangoVariety;
import com.sustainablefarm.service.BatchService;
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

    @Autowired
    public BatchController(BatchService batchService, DtoMapper dtoMapper) {
        this.batchService = batchService;
        this.dtoMapper = dtoMapper;
    }

    @PostMapping
    @Operation(summary = "Create a new batch", description = "Creates a new batch with the provided details")
    public ResponseEntity<BatchResponse> createBatch(@Valid @RequestBody BatchCreateRequest request) {
        Batch batch = dtoMapper.toEntity(request);
        Batch createdBatch = batchService.createBatch(batch);
        BatchResponse response = dtoMapper.toResponse(createdBatch);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{batchId}")
    @Operation(summary = "Get batch by ID", description = "Retrieves a specific batch by its ID")
    public ResponseEntity<BatchResponse> getBatchById(
            @Parameter(description = "Batch ID") @PathVariable String batchId) {
        Batch batch = batchService.getBatchById(batchId);
        BatchResponse response = dtoMapper.toResponse(batch);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all batches", description = "Retrieves all batches")
    public ResponseEntity<List<BatchResponse>> getAllBatches() {
        List<Batch> batches = batchService.getAllBatches();
        List<BatchResponse> responses = batches.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
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
        BatchResponse response = dtoMapper.toResponse(updatedBatch);
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
            @Valid @RequestBody BatchStatusAdvanceRequest request) {
        Batch batch = batchService.advanceBatchStatus(batchId);
        BatchResponse response = dtoMapper.toResponse(batch);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{batchId}/status")
    @Operation(summary = "Set batch status", description = "Manually sets batch status (for operations like REJECTED)")
    public ResponseEntity<BatchResponse> setBatchStatus(
            @Parameter(description = "Batch ID") @PathVariable String batchId,
            @Parameter(description = "New status") @RequestParam BatchStatus status) {
        Batch batch = batchService.setBatchStatus(batchId, status);
        BatchResponse response = dtoMapper.toResponse(batch);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get batches by status", description = "Retrieves batches filtered by status")
    public ResponseEntity<List<BatchResponse>> getBatchesByStatus(
            @Parameter(description = "Batch status") @PathVariable BatchStatus status) {
        List<Batch> batches = batchService.getBatchesByStatus(status);
        List<BatchResponse> responses = batches.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/farm/{farmId}")
    @Operation(summary = "Get batches by farm", description = "Retrieves batches filtered by farm ID")
    public ResponseEntity<List<BatchResponse>> getBatchesByFarm(
            @Parameter(description = "Farm ID") @PathVariable String farmId) {
        List<Batch> batches = batchService.getBatchesByFarm(farmId);
        List<BatchResponse> responses = batches.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/variety/{variety}")
    @Operation(summary = "Get batches by variety", description = "Retrieves batches filtered by mango variety")
    public ResponseEntity<List<BatchResponse>> getBatchesByVariety(
            @Parameter(description = "Mango variety") @PathVariable MangoVariety variety) {
        List<Batch> batches = batchService.getBatchesByVariety(variety);
        List<BatchResponse> responses = batches.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/ready-for-shipping")
    @Operation(summary = "Get batches ready for shipping", description = "Retrieves batches that are ready for shipping")
    public ResponseEntity<List<BatchResponse>> getBatchesReadyForShipping() {
        List<Batch> batches = batchService.getBatchesReadyForShipping();
        List<BatchResponse> responses = batches.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/farm/{farmId}/block/{blockId}")
    @Operation(summary = "Get batches by farm and block", description = "Retrieves batches filtered by farm and block ID")
    public ResponseEntity<List<BatchResponse>> getBatchesByFarmAndBlock(
            @Parameter(description = "Farm ID") @PathVariable String farmId,
            @Parameter(description = "Block ID") @PathVariable String blockId) {
        List<Batch> batches = batchService.getBatchesByFarmAndBlock(farmId, blockId);
        List<BatchResponse> responses = batches.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get batches by harvest date range", description = "Retrieves batches within a harvest date range")
    public ResponseEntity<List<BatchResponse>> getBatchesByHarvestDateRange(
            @Parameter(description = "Start date") @RequestParam LocalDate startDate,
            @Parameter(description = "End date") @RequestParam LocalDate endDate) {
        List<Batch> batches = batchService.getBatchesByHarvestDateRange(startDate, endDate);
        List<BatchResponse> responses = batches.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}
