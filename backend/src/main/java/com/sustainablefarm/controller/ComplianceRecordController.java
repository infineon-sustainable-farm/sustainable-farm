package com.sustainablefarm.controller;

import com.sustainablefarm.dto.mapper.DtoMapper;
import com.sustainablefarm.dto.request.ComplianceRecordCreateRequest;
import com.sustainablefarm.dto.request.ComplianceRecordUpdateRequest;
import com.sustainablefarm.dto.response.ComplianceRecordResponse;
import com.sustainablefarm.model.Batch;
import com.sustainablefarm.model.ComplianceRecord;
import com.sustainablefarm.model.Operator;
import com.sustainablefarm.repository.BatchRepository;
import com.sustainablefarm.repository.OperatorRepository;
import com.sustainablefarm.service.ComplianceRecordService;
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
 * REST Controller for ComplianceRecord operations
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/compliance-records")
@Tag(name = "Compliance Management", description = "APIs for managing HACCP compliance records")
public class ComplianceRecordController {

    private final ComplianceRecordService complianceRecordService;
    private final BatchRepository batchRepository;
    private final OperatorRepository operatorRepository;
    private final DtoMapper dtoMapper;

    @Autowired
    public ComplianceRecordController(ComplianceRecordService complianceRecordService,
                                    BatchRepository batchRepository,
                                    OperatorRepository operatorRepository,
                                    DtoMapper dtoMapper) {
        this.complianceRecordService = complianceRecordService;
        this.batchRepository = batchRepository;
        this.operatorRepository = operatorRepository;
        this.dtoMapper = dtoMapper;
    }

    @PostMapping
    @Operation(summary = "Create a new compliance record", description = "Creates a new HACCP compliance record")
    public ResponseEntity<ComplianceRecordResponse> createComplianceRecord(@Valid @RequestBody ComplianceRecordCreateRequest request) {
        Batch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new IllegalArgumentException("Batch not found with ID: " + request.getBatchId()));
        
        Operator auditor = null;
        if (request.getAuditorId() != null) {
            auditor = operatorRepository.findById(request.getAuditorId())
                    .orElseThrow(() -> new IllegalArgumentException("Operator not found with ID: " + request.getAuditorId()));
        }
        
        ComplianceRecord record = dtoMapper.toEntity(request, batch, auditor);
        ComplianceRecord createdRecord = complianceRecordService.createComplianceRecord(record);
        ComplianceRecordResponse response = dtoMapper.toResponse(createdRecord);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{recordId}")
    @Operation(summary = "Get compliance record by ID", description = "Retrieves a specific compliance record by its ID")
    public ResponseEntity<ComplianceRecordResponse> getComplianceRecordById(
            @Parameter(description = "Record ID") @PathVariable String recordId) {
        ComplianceRecord record = complianceRecordService.getComplianceRecordById(recordId);
        ComplianceRecordResponse response = dtoMapper.toResponse(record);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/batch/{batchId}")
    @Operation(summary = "Get compliance records by batch", description = "Retrieves compliance records for a specific batch")
    public ResponseEntity<List<ComplianceRecordResponse>> getComplianceRecordsByBatch(
            @Parameter(description = "Batch ID") @PathVariable String batchId) {
        List<ComplianceRecord> records = complianceRecordService.getComplianceRecordsByBatch(batchId);
        List<ComplianceRecordResponse> responses = records.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping
    @Operation(summary = "Get all compliance records", description = "Retrieves all compliance records")
    public ResponseEntity<List<ComplianceRecordResponse>> getAllComplianceRecords() {
        List<ComplianceRecord> records = complianceRecordService.getAllComplianceRecords();
        List<ComplianceRecordResponse> responses = records.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{recordId}")
    @Operation(summary = "Update compliance record", description = "Updates an existing compliance record")
    public ResponseEntity<ComplianceRecordResponse> updateComplianceRecord(
            @Parameter(description = "Record ID") @PathVariable String recordId,
            @Valid @RequestBody ComplianceRecordUpdateRequest request) {
        ComplianceRecord existingRecord = complianceRecordService.getComplianceRecordById(recordId);
        
        if (request.getComplianceType() != null) {
            existingRecord.setComplianceType(request.getComplianceType());
        }
        if (request.getRequirement() != null) {
            existingRecord.setRequirement(request.getRequirement());
        }
        if (request.getResult() != null) {
            existingRecord.setResult(request.getResult());
        }
        if (request.getEvidence() != null) {
            existingRecord.setEvidence(request.getEvidence());
        }
        if (request.getAuditorId() != null) {
            Operator auditor = operatorRepository.findById(request.getAuditorId())
                    .orElseThrow(() -> new IllegalArgumentException("Operator not found with ID: " + request.getAuditorId()));
            existingRecord.setAuditor(auditor);
        }
        if (request.getAuditDate() != null) {
            existingRecord.setAuditDate(request.getAuditDate());
        }
        if (request.getNextAuditDate() != null) {
            existingRecord.setNextAuditDate(request.getNextAuditDate());
        }
        
        ComplianceRecord updatedRecord = complianceRecordService.updateComplianceRecord(recordId, existingRecord);
        ComplianceRecordResponse response = dtoMapper.toResponse(updatedRecord);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{recordId}")
    @Operation(summary = "Delete compliance record", description = "Deletes a compliance record by its ID")
    public ResponseEntity<Void> deleteComplianceRecord(
            @Parameter(description = "Record ID") @PathVariable String recordId) {
        complianceRecordService.deleteComplianceRecord(recordId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/type/{complianceType}")
    @Operation(summary = "Get compliance records by type", description = "Retrieves compliance records filtered by compliance type")
    public ResponseEntity<List<ComplianceRecordResponse>> getComplianceRecordsByType(
            @Parameter(description = "Compliance type") @PathVariable com.sustainablefarm.model.ComplianceRecord.ComplianceType complianceType) {
        List<ComplianceRecord> records = complianceRecordService.getComplianceRecordsByType(complianceType);
        List<ComplianceRecordResponse> responses = records.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/result/{result}")
    @Operation(summary = "Get compliance records by result", description = "Retrieves compliance records filtered by result")
    public ResponseEntity<List<ComplianceRecordResponse>> getComplianceRecordsByResult(
            @Parameter(description = "Compliance result") @PathVariable com.sustainablefarm.model.ComplianceRecord.ComplianceResult result) {
        List<ComplianceRecord> records = complianceRecordService.getComplianceRecordsByResult(result);
        List<ComplianceRecordResponse> responses = records.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/auditor/{auditorId}")
    @Operation(summary = "Get compliance records by auditor", description = "Retrieves compliance records filtered by auditor")
    public ResponseEntity<List<ComplianceRecordResponse>> getComplianceRecordsByAuditor(
            @Parameter(description = "Auditor ID") @PathVariable String auditorId) {
        List<ComplianceRecord> records = complianceRecordService.getComplianceRecordsByAuditor(auditorId);
        List<ComplianceRecordResponse> responses = records.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/overdue")
    @Operation(summary = "Get overdue compliance records", description = "Retrieves compliance records with overdue next audit dates")
    public ResponseEntity<List<ComplianceRecordResponse>> getOverdueComplianceRecords() {
        List<ComplianceRecord> records = complianceRecordService.getOverdueAudits();
        List<ComplianceRecordResponse> responses = records.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get compliance records by audit date range", description = "Retrieves compliance records within an audit date range")
    public ResponseEntity<List<ComplianceRecordResponse>> getComplianceRecordsByDateRange(
            @Parameter(description = "Start date") @RequestParam LocalDate startDate,
            @Parameter(description = "End date") @RequestParam LocalDate endDate) {
        List<ComplianceRecord> records = complianceRecordService.getComplianceRecordsByDateRange(startDate, endDate);
        List<ComplianceRecordResponse> responses = records.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}
