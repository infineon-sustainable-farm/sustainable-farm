package com.sustainablefarm.controller;

import com.sustainablefarm.dto.mapper.DtoMapper;
import com.sustainablefarm.dto.request.RawIntakeCreateRequest;
import com.sustainablefarm.dto.request.RawIntakeUpdateRequest;
import com.sustainablefarm.dto.response.RawIntakeResponse;
import com.sustainablefarm.model.Batch;
import com.sustainablefarm.model.HarvestEvent.MangoVariety;
import com.sustainablefarm.model.HarvestEvent.QualityGrade;
import com.sustainablefarm.model.RawIntake;
import com.sustainablefarm.repository.BatchRepository;
import com.sustainablefarm.service.RawIntakeService;
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
 * REST Controller for RawIntake operations
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/raw-intakes")
@Tag(name = "Raw Intake Management", description = "APIs for managing raw material intake from Plants")
public class RawIntakeController {

    private final RawIntakeService rawIntakeService;
    private final BatchRepository batchRepository;
    private final DtoMapper dtoMapper;

    @Autowired
    public RawIntakeController(RawIntakeService rawIntakeService, 
                              BatchRepository batchRepository,
                              DtoMapper dtoMapper) {
        this.rawIntakeService = rawIntakeService;
        this.batchRepository = batchRepository;
        this.dtoMapper = dtoMapper;
    }

    @PostMapping
    @Operation(summary = "Create a new raw intake", description = "Creates a new raw intake record")
    public ResponseEntity<RawIntakeResponse> createRawIntake(@Valid @RequestBody RawIntakeCreateRequest request) {
        Batch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new IllegalArgumentException("Batch not found with ID: " + request.getBatchId()));
        
        RawIntake intake = dtoMapper.toEntity(request, batch);
        RawIntake createdIntake = rawIntakeService.createRawIntake(intake);
        RawIntakeResponse response = dtoMapper.toResponse(createdIntake);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{intakeId}")
    @Operation(summary = "Get raw intake by ID", description = "Retrieves a specific raw intake by its ID")
    public ResponseEntity<RawIntakeResponse> getRawIntakeById(
            @Parameter(description = "Intake ID") @PathVariable String intakeId) {
        RawIntake intake = rawIntakeService.getRawIntakeById(intakeId);
        RawIntakeResponse response = dtoMapper.toResponse(intake);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/batch/{batchId}")
    @Operation(summary = "Get raw intake by batch ID", description = "Retrieves raw intake for a specific batch")
    public ResponseEntity<RawIntakeResponse> getRawIntakeByBatchId(
            @Parameter(description = "Batch ID") @PathVariable String batchId) {
        RawIntake intake = rawIntakeService.getRawIntakeByBatchId(batchId);
        RawIntakeResponse response = dtoMapper.toResponse(intake);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all raw intakes", description = "Retrieves all raw intake records")
    public ResponseEntity<List<RawIntakeResponse>> getAllRawIntakes() {
        List<RawIntake> intakes = rawIntakeService.getAllRawIntakes();
        List<RawIntakeResponse> responses = intakes.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{intakeId}")
    @Operation(summary = "Update raw intake", description = "Updates an existing raw intake")
    public ResponseEntity<RawIntakeResponse> updateRawIntake(
            @Parameter(description = "Intake ID") @PathVariable String intakeId,
            @Valid @RequestBody RawIntakeUpdateRequest request) {
        RawIntake existingIntake = rawIntakeService.getRawIntakeById(intakeId);
        
        existingIntake.setSourceFarm(request.getSourceFarm());
        existingIntake.setSourceBlock(request.getSourceBlock());
        existingIntake.setIntakeDate(request.getIntakeDate());
        existingIntake.setReceivedQuantityKg(request.getReceivedQuantityKg());
        existingIntake.setReceivedVariety(request.getReceivedVariety());
        existingIntake.setReceivedGrade(request.getReceivedGrade());
        existingIntake.setIntakeOperator(request.getIntakeOperator());
        
        RawIntake updatedIntake = rawIntakeService.updateRawIntake(intakeId, existingIntake);
        RawIntakeResponse response = dtoMapper.toResponse(updatedIntake);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{intakeId}")
    @Operation(summary = "Delete raw intake", description = "Deletes a raw intake by its ID")
    public ResponseEntity<Void> deleteRawIntake(
            @Parameter(description = "Intake ID") @PathVariable String intakeId) {
        rawIntakeService.deleteRawIntake(intakeId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{intakeId}/initialize-batch")
    @Operation(summary = "Initialize batch from intake", description = "Initializes batch status to INTAKE from raw intake")
    public ResponseEntity<RawIntakeResponse> initializeBatchFromIntake(
            @Parameter(description = "Intake ID") @PathVariable String intakeId) {
        RawIntake intake = rawIntakeService.initializeBatchFromIntake(intakeId);
        RawIntakeResponse response = dtoMapper.toResponse(intake);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get raw intakes by date range", description = "Retrieves raw intakes within a date range")
    public ResponseEntity<List<RawIntakeResponse>> getRawIntakesByDateRange(
            @Parameter(description = "Start date") @RequestParam LocalDate startDate,
            @Parameter(description = "End date") @RequestParam LocalDate endDate) {
        List<RawIntake> intakes = rawIntakeService.getRawIntakesByDateRange(startDate, endDate);
        List<RawIntakeResponse> responses = intakes.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/farm/{sourceFarm}")
    @Operation(summary = "Get raw intakes by source farm", description = "Retrieves raw intakes filtered by source farm")
    public ResponseEntity<List<RawIntakeResponse>> getRawIntakesBySourceFarm(
            @Parameter(description = "Source farm") @PathVariable String sourceFarm) {
        List<RawIntake> intakes = rawIntakeService.getRawIntakesBySourceFarm(sourceFarm);
        List<RawIntakeResponse> responses = intakes.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/variety/{variety}")
    @Operation(summary = "Get raw intakes by variety", description = "Retrieves raw intakes filtered by mango variety")
    public ResponseEntity<List<RawIntakeResponse>> getRawIntakesByVariety(
            @Parameter(description = "Mango variety") @PathVariable MangoVariety variety) {
        List<RawIntake> intakes = rawIntakeService.getRawIntakesByVariety(variety);
        List<RawIntakeResponse> responses = intakes.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/grade/{grade}")
    @Operation(summary = "Get raw intakes by grade", description = "Retrieves raw intakes filtered by quality grade")
    public ResponseEntity<List<RawIntakeResponse>> getRawIntakesByGrade(
            @Parameter(description = "Quality grade") @PathVariable QualityGrade grade) {
        List<RawIntake> intakes = rawIntakeService.getRawIntakesByGrade(grade);
        List<RawIntakeResponse> responses = intakes.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}
