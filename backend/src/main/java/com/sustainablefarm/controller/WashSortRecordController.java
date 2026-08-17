package com.sustainablefarm.controller;

import com.sustainablefarm.dto.mapper.DtoMapper;
import com.sustainablefarm.dto.request.WashSortRecordCompleteRequest;
import com.sustainablefarm.dto.request.WashSortRecordCreateRequest;
import com.sustainablefarm.dto.request.WashSortRecordUpdateRequest;
import com.sustainablefarm.dto.response.WashSortRecordResponse;
import com.sustainablefarm.model.Batch;
import com.sustainablefarm.model.Equipment;
import com.sustainablefarm.model.Operator;
import com.sustainablefarm.model.WashSortRecord;
import com.sustainablefarm.repository.BatchRepository;
import com.sustainablefarm.repository.EquipmentRepository;
import com.sustainablefarm.repository.OperatorRepository;
import com.sustainablefarm.service.WashSortRecordService;
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
 * REST Controller for WashSortRecord operations
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/wash-sort-records")
@Tag(name = "Wash & Sort Management", description = "APIs for managing washing and sorting records")
public class WashSortRecordController {

    private final WashSortRecordService washSortRecordService;
    private final BatchRepository batchRepository;
    private final EquipmentRepository equipmentRepository;
    private final OperatorRepository operatorRepository;
    private final DtoMapper dtoMapper;

    @Autowired
    public WashSortRecordController(WashSortRecordService washSortRecordService,
                                   BatchRepository batchRepository,
                                   EquipmentRepository equipmentRepository,
                                   OperatorRepository operatorRepository,
                                   DtoMapper dtoMapper) {
        this.washSortRecordService = washSortRecordService;
        this.batchRepository = batchRepository;
        this.equipmentRepository = equipmentRepository;
        this.operatorRepository = operatorRepository;
        this.dtoMapper = dtoMapper;
    }

    @PostMapping
    @Operation(summary = "Create a new wash sort record", description = "Creates a new washing and sorting record")
    public ResponseEntity<WashSortRecordResponse> createWashSortRecord(@Valid @RequestBody WashSortRecordCreateRequest request) {
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
        
        WashSortRecord record = dtoMapper.toEntity(request, batch, equipment, operator);
        WashSortRecord createdRecord = washSortRecordService.createWashSortRecord(record);
        WashSortRecordResponse response = dtoMapper.toResponse(createdRecord);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{recordId}")
    @Operation(summary = "Get wash sort record by ID", description = "Retrieves a specific wash sort record by its ID")
    public ResponseEntity<WashSortRecordResponse> getWashSortRecordById(
            @Parameter(description = "Record ID") @PathVariable String recordId) {
        WashSortRecord record = washSortRecordService.getWashSortRecordById(recordId);
        WashSortRecordResponse response = dtoMapper.toResponse(record);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/batch/{batchId}")
    @Operation(summary = "Get wash sort records by batch", description = "Retrieves wash sort records for a specific batch")
    public ResponseEntity<List<WashSortRecordResponse>> getWashSortRecordsByBatch(
            @Parameter(description = "Batch ID") @PathVariable String batchId) {
        List<WashSortRecord> records = washSortRecordService.getWashSortRecordsByBatch(batchId);
        List<WashSortRecordResponse> responses = records.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping
    @Operation(summary = "Get all wash sort records", description = "Retrieves all wash sort records")
    public ResponseEntity<List<WashSortRecordResponse>> getAllWashSortRecords() {
        List<WashSortRecord> records = washSortRecordService.getAllWashSortRecords();
        List<WashSortRecordResponse> responses = records.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{recordId}")
    @Operation(summary = "Update wash sort record", description = "Updates an existing wash sort record")
    public ResponseEntity<WashSortRecordResponse> updateWashSortRecord(
            @Parameter(description = "Record ID") @PathVariable String recordId,
            @Valid @RequestBody WashSortRecordUpdateRequest request) {
        WashSortRecord existingRecord = washSortRecordService.getWashSortRecordById(recordId);
        
        if (request.getInputQuantityKg() != null) {
            existingRecord.setInputQuantityKg(request.getInputQuantityKg());
        }
        if (request.getOutputQuantityKg() != null) {
            existingRecord.setOutputQuantityKg(request.getOutputQuantityKg());
        }
        if (request.getWasteQuantityKg() != null) {
            existingRecord.setWasteQuantityKg(request.getWasteQuantityKg());
        }
        if (request.getWaterUsageLiters() != null) {
            existingRecord.setWaterUsageLiters(request.getWaterUsageLiters());
        }
        if (request.getStartTime() != null) {
            existingRecord.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            existingRecord.setEndTime(request.getEndTime());
        }
        if (request.getEquipmentId() != null) {
            Equipment equipment = equipmentRepository.findById(request.getEquipmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Equipment not found with ID: " + request.getEquipmentId()));
            existingRecord.setEquipment(equipment);
        }
        if (request.getOperatorId() != null) {
            Operator operator = operatorRepository.findById(request.getOperatorId())
                    .orElseThrow(() -> new IllegalArgumentException("Operator not found with ID: " + request.getOperatorId()));
            existingRecord.setOperator(operator);
        }
        
        WashSortRecord updatedRecord = washSortRecordService.updateWashSortRecord(recordId, existingRecord);
        WashSortRecordResponse response = dtoMapper.toResponse(updatedRecord);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{recordId}")
    @Operation(summary = "Delete wash sort record", description = "Deletes a wash sort record by its ID")
    public ResponseEntity<Void> deleteWashSortRecord(
            @Parameter(description = "Record ID") @PathVariable String recordId) {
        washSortRecordService.deleteWashSortRecord(recordId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{recordId}/complete")
    @Operation(summary = "Complete wash sort record", description = "Completes a wash sort record with final quantities")
    public ResponseEntity<WashSortRecordResponse> completeWashSortRecord(
            @Parameter(description = "Record ID") @PathVariable String recordId,
            @Valid @RequestBody WashSortRecordCompleteRequest request) {
        WashSortRecord record = washSortRecordService.completeWashSortRecord(
                recordId, request.getOutputQuantityKg(), request.getWasteQuantityKg());
        WashSortRecordResponse response = dtoMapper.toResponse(record);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get wash sort records by date range", description = "Retrieves wash sort records within a date range")
    public ResponseEntity<List<WashSortRecordResponse>> getWashSortRecordsByDateRange(
            @Parameter(description = "Start date") @RequestParam LocalDateTime startDate,
            @Parameter(description = "End date") @RequestParam LocalDateTime endDate) {
        List<WashSortRecord> records = washSortRecordService.getWashSortRecordsByDateRange(startDate, endDate);
        List<WashSortRecordResponse> responses = records.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/equipment/{equipmentId}")
    @Operation(summary = "Get wash sort records by equipment", description = "Retrieves wash sort records filtered by equipment")
    public ResponseEntity<List<WashSortRecordResponse>> getWashSortRecordsByEquipment(
            @Parameter(description = "Equipment ID") @PathVariable String equipmentId) {
        List<WashSortRecord> records = washSortRecordService.getWashSortRecordsByEquipment(equipmentId);
        List<WashSortRecordResponse> responses = records.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/operator/{operatorId}")
    @Operation(summary = "Get wash sort records by operator", description = "Retrieves wash sort records filtered by operator")
    public ResponseEntity<List<WashSortRecordResponse>> getWashSortRecordsByOperator(
            @Parameter(description = "Operator ID") @PathVariable String operatorId) {
        List<WashSortRecord> records = washSortRecordService.getWashSortRecordsByOperator(operatorId);
        List<WashSortRecordResponse> responses = records.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}
