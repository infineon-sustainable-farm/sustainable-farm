package com.sustainablefarm.controller;

import com.sustainablefarm.dto.mapper.DtoMapper;
import com.sustainablefarm.dto.request.ComplianceRecordCompleteAuditRequest;
import com.sustainablefarm.dto.request.ComplianceRecordCreateRequest;
import com.sustainablefarm.dto.request.ComplianceRecordScheduleAuditRequest;
import com.sustainablefarm.dto.request.ComplianceRecordUpdateRequest;
import com.sustainablefarm.dto.response.ComplianceRecordResponse;
import com.sustainablefarm.dto.response.PageResponse;
import com.sustainablefarm.model.Batch;
import com.sustainablefarm.model.ComplianceRecord;
import com.sustainablefarm.model.ComplianceRecord.ComplianceResult;
import com.sustainablefarm.model.ComplianceRecord.ComplianceType;
import com.sustainablefarm.model.Operator;
import com.sustainablefarm.repository.BatchRepository;
import com.sustainablefarm.repository.OperatorRepository;
import com.sustainablefarm.service.ComplianceRecordService;
import com.sustainablefarm.util.PaginationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/compliance-records")
@Tag(name = "Compliance Management", description = "APIs for HACCP compliance records and audits")
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
    @Operation(summary = "Create compliance record")
    public ResponseEntity<ComplianceRecordResponse> createComplianceRecord(
            @Valid @RequestBody ComplianceRecordCreateRequest request) {
        Batch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new IllegalArgumentException("Batch not found with ID: " + request.getBatchId()));
        Operator auditor = resolveAuditor(request.getAuditorId());

        ComplianceRecord record = dtoMapper.toEntity(request, batch, auditor);
        ComplianceRecord created = complianceRecordService.createComplianceRecord(record);
        return ResponseEntity.status(HttpStatus.CREATED).body(dtoMapper.toResponse(created));
    }

    @GetMapping("/{recordId}")
    @Operation(summary = "Get compliance record by ID")
    public ResponseEntity<ComplianceRecordResponse> getComplianceRecordById(@PathVariable String recordId) {
        return ResponseEntity.ok(dtoMapper.toResponse(complianceRecordService.getComplianceRecordById(recordId)));
    }

    @GetMapping
    @Operation(summary = "Get all compliance records (paginated)")
    public ResponseEntity<PageResponse<ComplianceRecordResponse>> getAllComplianceRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<ComplianceRecordResponse> responses = complianceRecordService.getAllComplianceRecords().stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(PaginationUtils.paginate(responses, page, size));
    }

    @PutMapping("/{recordId}")
    @Operation(summary = "Update compliance record")
    public ResponseEntity<ComplianceRecordResponse> updateComplianceRecord(
            @PathVariable String recordId,
            @Valid @RequestBody ComplianceRecordUpdateRequest request) {
        ComplianceRecord existing = complianceRecordService.getComplianceRecordById(recordId);
        applyUpdate(existing, request);
        ComplianceRecord updated = complianceRecordService.updateComplianceRecord(recordId, existing);
        return ResponseEntity.ok(dtoMapper.toResponse(updated));
    }

    @DeleteMapping("/{recordId}")
    @Operation(summary = "Delete compliance record")
    public ResponseEntity<Void> deleteComplianceRecord(@PathVariable String recordId) {
        complianceRecordService.deleteComplianceRecord(recordId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{recordId}/complete-audit")
    @Operation(summary = "Complete compliance audit")
    public ResponseEntity<ComplianceRecordResponse> completeAudit(
            @PathVariable String recordId,
            @Valid @RequestBody ComplianceRecordCompleteAuditRequest request) {
        ComplianceRecord record = complianceRecordService.completeAudit(
                recordId, request.getResult(), request.getEvidence());
        return ResponseEntity.ok(dtoMapper.toResponse(record));
    }

    @PostMapping("/{recordId}/schedule-audit")
    @Operation(summary = "Schedule next compliance audit")
    public ResponseEntity<ComplianceRecordResponse> scheduleNextAudit(
            @PathVariable String recordId,
            @Valid @RequestBody ComplianceRecordScheduleAuditRequest request) {
        ComplianceRecord record = complianceRecordService.scheduleNextAudit(recordId, request.getNextAuditDate());
        return ResponseEntity.ok(dtoMapper.toResponse(record));
    }

    @GetMapping("/batch/{batchId}")
    @Operation(summary = "Get compliance records by batch")
    public ResponseEntity<List<ComplianceRecordResponse>> getComplianceRecordsByBatch(@PathVariable String batchId) {
        List<ComplianceRecordResponse> responses = complianceRecordService.getComplianceRecordsByBatch(batchId).stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/overdue")
    @Operation(summary = "Get overdue compliance audits")
    public ResponseEntity<List<ComplianceRecordResponse>> getOverdueAudits() {
        List<ComplianceRecordResponse> responses = complianceRecordService.getOverdueAudits().stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending compliance records")
    public ResponseEntity<List<ComplianceRecordResponse>> getPendingComplianceRecords() {
        List<ComplianceRecordResponse> responses = complianceRecordService.getPendingComplianceRecords().stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get compliance records by type")
    public ResponseEntity<List<ComplianceRecordResponse>> getComplianceRecordsByType(@PathVariable ComplianceType type) {
        List<ComplianceRecordResponse> responses = complianceRecordService.getComplianceRecordsByType(type).stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/result/{result}")
    @Operation(summary = "Get compliance records by result")
    public ResponseEntity<List<ComplianceRecordResponse>> getComplianceRecordsByResult(
            @PathVariable ComplianceResult result) {
        List<ComplianceRecordResponse> responses = complianceRecordService.getComplianceRecordsByResult(result).stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get compliance records by audit date range")
    public ResponseEntity<List<ComplianceRecordResponse>> getComplianceRecordsByDateRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        List<ComplianceRecordResponse> responses = complianceRecordService
                .getComplianceRecordsByDateRange(startDate, endDate).stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    private void applyUpdate(ComplianceRecord existing, ComplianceRecordUpdateRequest request) {
        if (request.getComplianceType() != null) {
            existing.setComplianceType(request.getComplianceType());
        }
        if (request.getRequirement() != null) {
            existing.setRequirement(request.getRequirement());
        }
        if (request.getResult() != null) {
            existing.setResult(request.getResult());
        }
        if (request.getEvidence() != null) {
            existing.setEvidence(request.getEvidence());
        }
        if (request.getAuditorId() != null) {
            existing.setAuditor(resolveAuditor(request.getAuditorId()));
        }
        if (request.getAuditDate() != null) {
            existing.setAuditDate(request.getAuditDate());
        }
        if (request.getNextAuditDate() != null) {
            existing.setNextAuditDate(request.getNextAuditDate());
        }
    }

    private Operator resolveAuditor(String auditorId) {
        if (auditorId == null) {
            return null;
        }
        return operatorRepository.findById(auditorId)
                .orElseThrow(() -> new IllegalArgumentException("Auditor not found with ID: " + auditorId));
    }
}
