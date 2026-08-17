package com.sustainablefarm.controller;

import com.sustainablefarm.dto.mapper.DtoMapper;
import com.sustainablefarm.dto.request.PackagingRecordCreateRequest;
import com.sustainablefarm.dto.request.PackagingRecordExportReadyRequest;
import com.sustainablefarm.dto.request.PackagingRecordUpdateRequest;
import com.sustainablefarm.dto.response.PackagingRecordResponse;
import com.sustainablefarm.dto.response.PageResponse;
import com.sustainablefarm.model.Batch;
import com.sustainablefarm.model.Equipment;
import com.sustainablefarm.model.Operator;
import com.sustainablefarm.model.PackagingRecord;
import com.sustainablefarm.repository.BatchRepository;
import com.sustainablefarm.repository.EquipmentRepository;
import com.sustainablefarm.repository.OperatorRepository;
import com.sustainablefarm.service.PackagingRecordService;
import com.sustainablefarm.util.PaginationUtils;
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

@RestController
@RequestMapping("/api/packaging-records")
@Tag(name = "Packaging Management", description = "APIs for packaging records and export readiness")
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
    @Operation(summary = "Create packaging record")
    public ResponseEntity<PackagingRecordResponse> createPackagingRecord(
            @Valid @RequestBody PackagingRecordCreateRequest request) {
        Batch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new IllegalArgumentException("Batch not found with ID: " + request.getBatchId()));

        Equipment equipment = resolveEquipment(request.getEquipmentId());
        Operator operator = resolveOperator(request.getOperatorId());

        PackagingRecord record = dtoMapper.toEntity(request, batch, equipment, operator);
        PackagingRecord created = packagingRecordService.createPackagingRecord(record);
        return ResponseEntity.status(HttpStatus.CREATED).body(dtoMapper.toResponse(created));
    }

    @GetMapping("/{recordId}")
    @Operation(summary = "Get packaging record by ID")
    public ResponseEntity<PackagingRecordResponse> getPackagingRecordById(
            @PathVariable String recordId) {
        return ResponseEntity.ok(dtoMapper.toResponse(packagingRecordService.getPackagingRecordById(recordId)));
    }

    @GetMapping
    @Operation(summary = "Get all packaging records (paginated)")
    public ResponseEntity<PageResponse<PackagingRecordResponse>> getAllPackagingRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<PackagingRecordResponse> responses = packagingRecordService.getAllPackagingRecords().stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(PaginationUtils.paginate(responses, page, size));
    }

    @PutMapping("/{recordId}")
    @Operation(summary = "Update packaging record")
    public ResponseEntity<PackagingRecordResponse> updatePackagingRecord(
            @PathVariable String recordId,
            @Valid @RequestBody PackagingRecordUpdateRequest request) {
        PackagingRecord existing = packagingRecordService.getPackagingRecordById(recordId);
        applyUpdate(existing, request);
        PackagingRecord updated = packagingRecordService.updatePackagingRecord(recordId, existing);
        return ResponseEntity.ok(dtoMapper.toResponse(updated));
    }

    @DeleteMapping("/{recordId}")
    @Operation(summary = "Delete packaging record")
    public ResponseEntity<Void> deletePackagingRecord(@PathVariable String recordId) {
        packagingRecordService.deletePackagingRecord(recordId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{recordId}/export-ready")
    @Operation(summary = "Mark packaging record as export ready")
    public ResponseEntity<PackagingRecordResponse> markAsExportReady(
            @PathVariable String recordId,
            @Valid @RequestBody(required = false) PackagingRecordExportReadyRequest request) {
        PackagingRecord record = packagingRecordService.markAsExportReady(recordId);
        return ResponseEntity.ok(dtoMapper.toResponse(record));
    }

    @GetMapping("/batch/{batchId}")
    @Operation(summary = "Get packaging records by batch")
    public ResponseEntity<List<PackagingRecordResponse>> getPackagingRecordsByBatch(
            @PathVariable String batchId) {
        List<PackagingRecordResponse> responses = packagingRecordService.getPackagingRecordsByBatch(batchId).stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/export-ready")
    @Operation(summary = "Get export-ready packaging records")
    public ResponseEntity<List<PackagingRecordResponse>> getExportReadyPackagingRecords() {
        List<PackagingRecordResponse> responses = packagingRecordService.getExportReadyPackagingRecords().stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get packaging records by date range")
    public ResponseEntity<List<PackagingRecordResponse>> getPackagingRecordsByDateRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        List<PackagingRecordResponse> responses = packagingRecordService
                .getPackagingRecordsByDateRange(startDate, endDate).stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/lot-code/{lotCode}")
    @Operation(summary = "Get packaging records by lot code")
    public ResponseEntity<List<PackagingRecordResponse>> getPackagingRecordsByLotCode(
            @PathVariable String lotCode) {
        List<PackagingRecordResponse> responses = packagingRecordService.getPackagingRecordsByLotCode(lotCode).stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/lot-code/{lotCode}/validate")
    @Operation(summary = "Validate lot code uniqueness")
    public ResponseEntity<Boolean> validateLotCode(@PathVariable String lotCode) {
        return ResponseEntity.ok(packagingRecordService.validateLotCode(lotCode));
    }

    private void applyUpdate(PackagingRecord existing, PackagingRecordUpdateRequest request) {
        if (request.getPackageType() != null) {
            existing.setPackageType(request.getPackageType());
        }
        if (request.getPackageQuantityKg() != null) {
            existing.setPackageQuantityKg(request.getPackageQuantityKg());
        }
        if (request.getExportReady() != null) {
            existing.setExportReady(request.getExportReady());
        }
        if (request.getPackagingDate() != null) {
            existing.setPackagingDate(request.getPackagingDate());
        }
        if (request.getEquipmentId() != null) {
            existing.setEquipment(resolveEquipment(request.getEquipmentId()));
        }
        if (request.getOperatorId() != null) {
            existing.setOperator(resolveOperator(request.getOperatorId()));
        }
    }

    private Equipment resolveEquipment(String equipmentId) {
        if (equipmentId == null) {
            return null;
        }
        return equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Equipment not found with ID: " + equipmentId));
    }

    private Operator resolveOperator(String operatorId) {
        if (operatorId == null) {
            return null;
        }
        return operatorRepository.findById(operatorId)
                .orElseThrow(() -> new IllegalArgumentException("Operator not found with ID: " + operatorId));
    }
}
