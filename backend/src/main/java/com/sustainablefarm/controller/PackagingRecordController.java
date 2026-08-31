package com.sustainablefarm.controller;

import com.sustainablefarm.core.dto.mapper.DtoMapper;
import com.sustainablefarm.dto.request.PackagingRecordCreateRequest;
import com.sustainablefarm.dto.request.PackagingRecordExportReadyRequest;
import com.sustainablefarm.dto.request.PackagingRecordUpdateRequest;
import com.sustainablefarm.dto.response.PackagingRecordResponse;
import com.sustainablefarm.model.Batch;
import com.sustainablefarm.model.Equipment;
import com.sustainablefarm.model.Operator;
import com.sustainablefarm.model.PackagingRecord;
import com.sustainablefarm.repository.BatchRepository;
import com.sustainablefarm.repository.EquipmentRepository;
import com.sustainablefarm.repository.OperatorRepository;
import com.sustainablefarm.service.PackagingRecordService;
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
 * REST Controller for PackagingRecord operations
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/packaging-records")
@Tag(name = "Packaging Management", description = "APIs for managing packaging records")
public class PackagingRecordController {

    private final PackagingRecordService packagingRecordService;
    private final BatchRepository batchRepository;
    private final EquipmentRepository equipmentRepository;
    private final OperatorRepository operatorRepository;
    private final DtoMapper dtoMapper;

    @Autowired
    public PackagingRecordController(PackagingRecordService packagingRecordService,
                                    BatchRepository batchRepository,
                                    EquipmentRepository equipmentRepository,
                                    OperatorRepository operatorRepository,
                                    DtoMapper dtoMapper) {
        this.packagingRecordService = packagingRecordService;
        this.batchRepository = batchRepository;
        this.equipmentRepository = equipmentRepository;
        this.operatorRepository = operatorRepository;
        this.dtoMapper = dtoMapper;
    }

    @PostMapping
    @Operation(summary = "Create a new packaging record", description = "Creates a new packaging record")
    public ResponseEntity<PackagingRecordResponse> createPackagingRecord(@Valid @RequestBody PackagingRecordCreateRequest request) {
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
        
        PackagingRecord record = dtoMapper.toEntity(request, batch, equipment, operator);
        PackagingRecord createdRecord = packagingRecordService.createPackagingRecord(record);
        PackagingRecordResponse response = dtoMapper.toResponse(createdRecord);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{recordId}")
    @Operation(summary = "Get packaging record by ID", description = "Retrieves a specific packaging record by its ID")
    public ResponseEntity<PackagingRecordResponse> getPackagingRecordById(
            @Parameter(description = "Record ID") @PathVariable String recordId) {
        PackagingRecord record = packagingRecordService.getPackagingRecordById(recordId);
        PackagingRecordResponse response = dtoMapper.toResponse(record);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/batch/{batchId}")
    @Operation(summary = "Get packaging records by batch", description = "Retrieves packaging records for a specific batch")
    public ResponseEntity<List<PackagingRecordResponse>> getPackagingRecordsByBatch(
            @Parameter(description = "Batch ID") @PathVariable String batchId) {
        List<PackagingRecord> records = packagingRecordService.getPackagingRecordsByBatch(batchId);
        List<PackagingRecordResponse> responses = records.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping
    @Operation(summary = "Get all packaging records", description = "Retrieves all packaging records")
    public ResponseEntity<List<PackagingRecordResponse>> getAllPackagingRecords() {
        List<PackagingRecord> records = packagingRecordService.getAllPackagingRecords();
        List<PackagingRecordResponse> responses = records.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{recordId}")
    @Operation(summary = "Update packaging record", description = "Updates an existing packaging record")
    public ResponseEntity<PackagingRecordResponse> updatePackagingRecord(
            @Parameter(description = "Record ID") @PathVariable String recordId,
            @Valid @RequestBody PackagingRecordUpdateRequest request) {
        PackagingRecord existingRecord = packagingRecordService.getPackagingRecordById(recordId);
        
        if (request.getPackageType() != null) {
            existingRecord.setPackageType(request.getPackageType());
        }
        if (request.getPackageQuantityKg() != null) {
            existingRecord.setPackageQuantityKg(request.getPackageQuantityKg());
        }
        if (request.getExportReady() != null) {
            existingRecord.setExportReady(request.getExportReady());
        }
        if (request.getPackagingDate() != null) {
            existingRecord.setPackagingDate(request.getPackagingDate());
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
        
        PackagingRecord updatedRecord = packagingRecordService.updatePackagingRecord(recordId, existingRecord);
        PackagingRecordResponse response = dtoMapper.toResponse(updatedRecord);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{recordId}")
    @Operation(summary = "Delete packaging record", description = "Deletes a packaging record by its ID")
    public ResponseEntity<Void> deletePackagingRecord(
            @Parameter(description = "Record ID") @PathVariable String recordId) {
        packagingRecordService.deletePackagingRecord(recordId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{recordId}/mark-export-ready")
    @Operation(summary = "Mark packaging record as export ready", description = "Marks a packaging record as ready for export")
    public ResponseEntity<PackagingRecordResponse> markExportReady(
            @Parameter(description = "Record ID") @PathVariable String recordId,
            @Valid @RequestBody PackagingRecordExportReadyRequest request) {
        PackagingRecord record = packagingRecordService.getPackagingRecordById(recordId);
        record.markAsExportReady();
        PackagingRecord updatedRecord = packagingRecordService.updatePackagingRecord(recordId, record);
        PackagingRecordResponse response = dtoMapper.toResponse(updatedRecord);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/export-ready")
    @Operation(summary = "Get export ready packaging records", description = "Retrieves packaging records marked as export ready")
    public ResponseEntity<List<PackagingRecordResponse>> getExportReadyPackagingRecords() {
        List<PackagingRecord> records = packagingRecordService.getExportReadyPackagingRecords();
        List<PackagingRecordResponse> responses = records.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}
