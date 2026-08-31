package com.sustainablefarm.controller;

import com.sustainablefarm.core.dto.mapper.DtoMapper;
import com.sustainablefarm.dto.request.QcCheckpointCreateRequest;
import com.sustainablefarm.dto.request.QcCheckpointUpdateRequest;
import com.sustainablefarm.dto.response.QcCheckpointResponse;
import com.sustainablefarm.model.Batch;
import com.sustainablefarm.model.Operator;
import com.sustainablefarm.model.QcCheckpoint;
import com.sustainablefarm.repository.BatchRepository;
import com.sustainablefarm.repository.OperatorRepository;
import com.sustainablefarm.service.QcCheckpointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for QcCheckpoint operations
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/qc-checkpoints")
@Tag(name = "Quality Control Management", description = "APIs for managing quality control checkpoints")
public class QcCheckpointController {

    private final QcCheckpointService qcCheckpointService;
    private final BatchRepository batchRepository;
    private final OperatorRepository operatorRepository;
    private final DtoMapper dtoMapper;

    @Autowired
    public QcCheckpointController(QcCheckpointService qcCheckpointService,
                                 BatchRepository batchRepository,
                                 OperatorRepository operatorRepository,
                                 DtoMapper dtoMapper) {
        this.qcCheckpointService = qcCheckpointService;
        this.batchRepository = batchRepository;
        this.operatorRepository = operatorRepository;
        this.dtoMapper = dtoMapper;
    }

    @PostMapping
    @Operation(summary = "Create a new QC checkpoint", description = "Creates a new quality control checkpoint")
    public ResponseEntity<QcCheckpointResponse> createQcCheckpoint(@Valid @RequestBody QcCheckpointCreateRequest request) {
        Batch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new IllegalArgumentException("Batch not found with ID: " + request.getBatchId()));
        
        Operator inspector = null;
        if (request.getInspectorId() != null) {
            inspector = operatorRepository.findById(request.getInspectorId())
                    .orElseThrow(() -> new IllegalArgumentException("Operator not found with ID: " + request.getInspectorId()));
        }
        
        QcCheckpoint checkpoint = dtoMapper.toEntity(request, batch, inspector);
        QcCheckpoint createdCheckpoint = qcCheckpointService.createQcCheckpoint(checkpoint);
        QcCheckpointResponse response = dtoMapper.toResponse(createdCheckpoint);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{checkpointId}")
    @Operation(summary = "Get QC checkpoint by ID", description = "Retrieves a specific QC checkpoint by its ID")
    public ResponseEntity<QcCheckpointResponse> getQcCheckpointById(
            @Parameter(description = "Checkpoint ID") @PathVariable String checkpointId) {
        QcCheckpoint checkpoint = qcCheckpointService.getQcCheckpointById(checkpointId);
        QcCheckpointResponse response = dtoMapper.toResponse(checkpoint);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/batch/{batchId}")
    @Operation(summary = "Get QC checkpoints by batch", description = "Retrieves QC checkpoints for a specific batch")
    public ResponseEntity<List<QcCheckpointResponse>> getQcCheckpointsByBatch(
            @Parameter(description = "Batch ID") @PathVariable String batchId) {
        List<QcCheckpoint> checkpoints = qcCheckpointService.getQcCheckpointsByBatch(batchId);
        List<QcCheckpointResponse> responses = checkpoints.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping
    @Operation(summary = "Get all QC checkpoints", description = "Retrieves all QC checkpoint records")
    public ResponseEntity<List<QcCheckpointResponse>> getAllQcCheckpoints() {
        List<QcCheckpoint> checkpoints = qcCheckpointService.getAllQcCheckpoints();
        List<QcCheckpointResponse> responses = checkpoints.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{checkpointId}")
    @Operation(summary = "Update QC checkpoint", description = "Updates an existing QC checkpoint")
    public ResponseEntity<QcCheckpointResponse> updateQcCheckpoint(
            @Parameter(description = "Checkpoint ID") @PathVariable String checkpointId,
            @Valid @RequestBody QcCheckpointUpdateRequest request) {
        QcCheckpoint existingCheckpoint = qcCheckpointService.getQcCheckpointById(checkpointId);
        
        if (request.getStage() != null) {
            existingCheckpoint.setStage(request.getStage());
        }
        if (request.getResult() != null) {
            existingCheckpoint.setResult(request.getResult());
        }
        if (request.getDefects() != null) {
            existingCheckpoint.setDefects(request.getDefects());
        }
        if (request.getDefectsCount() != null) {
            existingCheckpoint.setDefectsCount(request.getDefectsCount());
        }
        if (request.getInspectorId() != null) {
            Operator inspector = operatorRepository.findById(request.getInspectorId())
                    .orElseThrow(() -> new IllegalArgumentException("Operator not found with ID: " + request.getInspectorId()));
            existingCheckpoint.setInspector(inspector);
        }
        if (request.getCheckpointTime() != null) {
            existingCheckpoint.setCheckpointTime(request.getCheckpointTime());
        }
        if (request.getNotes() != null) {
            existingCheckpoint.setNotes(request.getNotes());
        }
        
        QcCheckpoint updatedCheckpoint = qcCheckpointService.updateQcCheckpoint(checkpointId, existingCheckpoint);
        QcCheckpointResponse response = dtoMapper.toResponse(updatedCheckpoint);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{checkpointId}")
    @Operation(summary = "Delete QC checkpoint", description = "Deletes a QC checkpoint by its ID")
    public ResponseEntity<Void> deleteQcCheckpoint(
            @Parameter(description = "Checkpoint ID") @PathVariable String checkpointId) {
        qcCheckpointService.deleteQcCheckpoint(checkpointId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/batch/{batchId}/stage/{stage}")
    @Operation(summary = "Get QC checkpoints by batch and stage", description = "Retrieves QC checkpoints for a specific batch and stage")
    public ResponseEntity<List<QcCheckpointResponse>> getQcCheckpointsByBatchAndStage(
            @Parameter(description = "Batch ID") @PathVariable String batchId,
            @Parameter(description = "QC stage") @PathVariable com.sustainablefarm.model.QcCheckpoint.QcStage stage) {
        List<QcCheckpoint> checkpoints = qcCheckpointService.getQcCheckpointsByBatchAndStage(batchId, stage);
        List<QcCheckpointResponse> responses = checkpoints.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/batch/{batchId}/count")
    @Operation(summary = "Count QC checkpoints by batch", description = "Counts QC checkpoints for a specific batch")
    public ResponseEntity<Long> countQcCheckpointsByBatch(
            @Parameter(description = "Batch ID") @PathVariable String batchId) {
        List<QcCheckpoint> checkpoints = qcCheckpointService.getQcCheckpointsByBatch(batchId);
        long count = checkpoints.size();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/batch/{batchId}/stage/{stage}/count")
    @Operation(summary = "Count QC checkpoints by batch and stage", description = "Counts QC checkpoints for a specific batch and stage")
    public ResponseEntity<Long> countQcCheckpointsByBatchAndStage(
            @Parameter(description = "Batch ID") @PathVariable String batchId,
            @Parameter(description = "QC stage") @PathVariable com.sustainablefarm.model.QcCheckpoint.QcStage stage) {
        long count = qcCheckpointService.countByBatchAndStage(batchId, stage);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get QC checkpoints by date range", description = "Retrieves QC checkpoints within a date range")
    public ResponseEntity<List<QcCheckpointResponse>> getQcCheckpointsByDateRange(
            @Parameter(description = "Start date") @RequestParam LocalDateTime startDate,
            @Parameter(description = "End date") @RequestParam LocalDateTime endDate) {
        List<QcCheckpoint> checkpoints = qcCheckpointService.getQcCheckpointsByDateRange(startDate, endDate);
        List<QcCheckpointResponse> responses = checkpoints.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/inspector/{inspectorId}")
    @Operation(summary = "Get QC checkpoints by inspector", description = "Retrieves QC checkpoints filtered by inspector")
    public ResponseEntity<List<QcCheckpointResponse>> getQcCheckpointsByInspector(
            @Parameter(description = "Inspector ID") @PathVariable String inspectorId) {
        List<QcCheckpoint> checkpoints = qcCheckpointService.getQcCheckpointsByInspector(inspectorId);
        List<QcCheckpointResponse> responses = checkpoints.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}
