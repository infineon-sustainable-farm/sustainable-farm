package com.sustainablefarm.controller;

import com.sustainablefarm.dto.mapper.DtoMapper;
import com.sustainablefarm.dto.request.DryingRunCreateRequest;
import com.sustainablefarm.dto.request.DryingRunUpdateRequest;
import com.sustainablefarm.dto.response.DryingRunResponse;
import com.sustainablefarm.model.Batch;
import com.sustainablefarm.model.DryingRun;
import com.sustainablefarm.model.Equipment;
import com.sustainablefarm.model.Operator;
import com.sustainablefarm.repository.BatchRepository;
import com.sustainablefarm.repository.EquipmentRepository;
import com.sustainablefarm.repository.OperatorRepository;
import com.sustainablefarm.service.DryingRunService;
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
 * REST Controller for DryingRun operations
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/drying-runs")
@Tag(name = "Drying Management", description = "APIs for managing drying process records")
public class DryingRunController {

    private final DryingRunService dryingRunService;
    private final BatchRepository batchRepository;
    private final EquipmentRepository equipmentRepository;
    private final OperatorRepository operatorRepository;
    private final DtoMapper dtoMapper;

    @Autowired
    public DryingRunController(DryingRunService dryingRunService,
                             BatchRepository batchRepository,
                             EquipmentRepository equipmentRepository,
                             OperatorRepository operatorRepository,
                             DtoMapper dtoMapper) {
        this.dryingRunService = dryingRunService;
        this.batchRepository = batchRepository;
        this.equipmentRepository = equipmentRepository;
        this.operatorRepository = operatorRepository;
        this.dtoMapper = dtoMapper;
    }

    @PostMapping
    @Operation(summary = "Create a new drying run", description = "Creates a new drying process record")
    public ResponseEntity<DryingRunResponse> createDryingRun(@Valid @RequestBody DryingRunCreateRequest request) {
        Batch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new IllegalArgumentException("Batch not found with ID: " + request.getBatchId()));
        
        Equipment equipment = null;
        if (request.getEquipmentId() != null) {
            equipment = equipmentRepository.findById(request.getEquipmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Equipment not found with ID: " + request.getEquipmentId()));
        }
        
        Operator operator = null;
        if (request.getOperatorId() != null) {
            operator = operatorRepository.findById(request.getOperatorId())
                    .orElseThrow(() -> new IllegalArgumentException("Operator not found with ID: " + request.getOperatorId()));
        }
        
        DryingRun run = dtoMapper.toEntity(request, batch, equipment, operator);
        DryingRun createdRun = dryingRunService.createDryingRun(run);
        DryingRunResponse response = dtoMapper.toResponse(createdRun);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{runId}")
    @Operation(summary = "Get drying run by ID", description = "Retrieves a specific drying run by its ID")
    public ResponseEntity<DryingRunResponse> getDryingRunById(
            @Parameter(description = "Run ID") @PathVariable String runId) {
        DryingRun run = dryingRunService.getDryingRunById(runId);
        DryingRunResponse response = dtoMapper.toResponse(run);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/batch/{batchId}")
    @Operation(summary = "Get drying runs by batch", description = "Retrieves drying runs for a specific batch")
    public ResponseEntity<List<DryingRunResponse>> getDryingRunsByBatch(
            @Parameter(description = "Batch ID") @PathVariable String batchId) {
        List<DryingRun> runs = dryingRunService.getDryingRunsByBatch(batchId);
        List<DryingRunResponse> responses = runs.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping
    @Operation(summary = "Get all drying runs", description = "Retrieves all drying run records")
    public ResponseEntity<List<DryingRunResponse>> getAllDryingRuns() {
        List<DryingRun> runs = dryingRunService.getAllDryingRuns();
        List<DryingRunResponse> responses = runs.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{runId}")
    @Operation(summary = "Update drying run", description = "Updates an existing drying run")
    public ResponseEntity<DryingRunResponse> updateDryingRun(
            @Parameter(description = "Run ID") @PathVariable String runId,
            @Valid @RequestBody DryingRunUpdateRequest request) {
        DryingRun existingRun = dryingRunService.getDryingRunById(runId);
        
        if (request.getDurationHours() != null) {
            existingRun.setDurationHours(request.getDurationHours());
        }
        if (request.getTargetTemperatureC() != null) {
            existingRun.setTargetTemperatureC(request.getTargetTemperatureC());
        }
        if (request.getActualTemperatureC() != null) {
            existingRun.setActualTemperatureC(request.getActualTemperatureC());
        }
        if (request.getStartMoisturePct() != null) {
            existingRun.setStartMoisturePct(request.getStartMoisturePct());
        }
        if (request.getEndMoisturePct() != null) {
            existingRun.setEndMoisturePct(request.getEndMoisturePct());
        }
        if (request.getEnergyUsageKwh() != null) {
            existingRun.setEnergyUsageKwh(request.getEnergyUsageKwh());
        }
        if (request.getStartTime() != null) {
            existingRun.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            existingRun.setEndTime(request.getEndTime());
        }
        if (request.getEquipmentId() != null) {
            Equipment equipment = equipmentRepository.findById(request.getEquipmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Equipment not found with ID: " + request.getEquipmentId()));
            existingRun.setEquipment(equipment);
        }
        if (request.getOperatorId() != null) {
            Operator operator = operatorRepository.findById(request.getOperatorId())
                    .orElseThrow(() -> new IllegalArgumentException("Operator not found with ID: " + request.getOperatorId()));
            existingRun.setOperator(operator);
        }
        
        DryingRun updatedRun = dryingRunService.updateDryingRun(runId, existingRun);
        DryingRunResponse response = dtoMapper.toResponse(updatedRun);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{runId}")
    @Operation(summary = "Delete drying run", description = "Deletes a drying run by its ID")
    public ResponseEntity<Void> deleteDryingRun(
            @Parameter(description = "Run ID") @PathVariable String runId) {
        dryingRunService.deleteDryingRun(runId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get drying runs by date range", description = "Retrieves drying runs within a date range")
    public ResponseEntity<List<DryingRunResponse>> getDryingRunsByDateRange(
            @Parameter(description = "Start date") @RequestParam LocalDateTime startDate,
            @Parameter(description = "End date") @RequestParam LocalDateTime endDate) {
        List<DryingRun> runs = dryingRunService.getDryingRunsByDateRange(startDate, endDate);
        List<DryingRunResponse> responses = runs.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/equipment/{equipmentId}")
    @Operation(summary = "Get drying runs by equipment", description = "Retrieves drying runs filtered by equipment")
    public ResponseEntity<List<DryingRunResponse>> getDryingRunsByEquipment(
            @Parameter(description = "Equipment ID") @PathVariable String equipmentId) {
        List<DryingRun> runs = dryingRunService.getDryingRunsByEquipment(equipmentId);
        List<DryingRunResponse> responses = runs.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/operator/{operatorId}")
    @Operation(summary = "Get drying runs by operator", description = "Retrieves drying runs filtered by operator")
    public ResponseEntity<List<DryingRunResponse>> getDryingRunsByOperator(
            @Parameter(description = "Operator ID") @PathVariable String operatorId) {
        List<DryingRun> runs = dryingRunService.getDryingRunsByOperator(operatorId);
        List<DryingRunResponse> responses = runs.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}
