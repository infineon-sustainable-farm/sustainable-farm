package com.sustainablefarm.controller;

import com.sustainablefarm.dto.mapper.DtoMapper;
import com.sustainablefarm.dto.request.QcCheckpointCreateRequest;
import com.sustainablefarm.dto.request.QcCheckpointMandatoryCreateRequest;
import com.sustainablefarm.dto.request.QcCheckpointUpdateRequest;
import com.sustainablefarm.dto.response.PageResponse;
import com.sustainablefarm.dto.response.QcCheckpointResponse;
import com.sustainablefarm.model.Batch;
import com.sustainablefarm.model.Operator;
import com.sustainablefarm.model.QcCheckpoint;
import com.sustainablefarm.model.QcCheckpoint.QcResult;
import com.sustainablefarm.model.QcCheckpoint.QcStage;
import com.sustainablefarm.repository.BatchRepository;
import com.sustainablefarm.repository.OperatorRepository;
import com.sustainablefarm.service.QcCheckpointService;
import com.sustainablefarm.util.PaginationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/qc-checkpoints")
@Tag(name = "Quality Control", description = "APIs for QC checkpoints and mandatory inspections")
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
    @Operation(summary = "Create QC checkpoint")
    public ResponseEntity<QcCheckpointResponse> createQcCheckpoint(
            @Valid @RequestBody QcCheckpointCreateRequest request) {
        Batch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new IllegalArgumentException("Batch not found with ID: " + request.getBatchId()));
        Operator inspector = resolveInspector(request.getInspectorId());

        QcCheckpoint checkpoint = dtoMapper.toEntity(request, batch, inspector);
        QcCheckpoint created = qcCheckpointService.createQcCheckpoint(checkpoint);
        return ResponseEntity.status(HttpStatus.CREATED).body(dtoMapper.toResponse(created));
    }

    @GetMapping("/{checkpointId}")
    @Operation(summary = "Get QC checkpoint by ID")
    public ResponseEntity<QcCheckpointResponse> getQcCheckpointById(@PathVariable String checkpointId) {
        return ResponseEntity.ok(dtoMapper.toResponse(qcCheckpointService.getQcCheckpointById(checkpointId)));
    }

    @GetMapping
    @Operation(summary = "Get all QC checkpoints (paginated)")
    public ResponseEntity<PageResponse<QcCheckpointResponse>> getAllQcCheckpoints(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<QcCheckpointResponse> responses = qcCheckpointService.getAllQcCheckpoints().stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(PaginationUtils.paginate(responses, page, size));
    }

    @PutMapping("/{checkpointId}")
    @Operation(summary = "Update QC checkpoint")
    public ResponseEntity<QcCheckpointResponse> updateQcCheckpoint(
            @PathVariable String checkpointId,
            @Valid @RequestBody QcCheckpointUpdateRequest request) {
        QcCheckpoint existing = qcCheckpointService.getQcCheckpointById(checkpointId);
        applyUpdate(existing, request);
        QcCheckpoint updated = qcCheckpointService.updateQcCheckpoint(checkpointId, existing);
        return ResponseEntity.ok(dtoMapper.toResponse(updated));
    }

    @DeleteMapping("/{checkpointId}")
    @Operation(summary = "Delete QC checkpoint")
    public ResponseEntity<Void> deleteQcCheckpoint(@PathVariable String checkpointId) {
        qcCheckpointService.deleteQcCheckpoint(checkpointId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/batch/{batchId}/mandatory")
    @Operation(summary = "Create mandatory QC checkpoint for batch")
    public ResponseEntity<QcCheckpointResponse> createMandatoryCheckpoint(
            @PathVariable String batchId,
            @Valid @RequestBody QcCheckpointMandatoryCreateRequest request) {
        QcCheckpoint checkpoint = qcCheckpointService.createMandatoryCheckpoint(
                batchId, request.getStage(), request.getInspectorId());
        return ResponseEntity.status(HttpStatus.CREATED).body(dtoMapper.toResponse(checkpoint));
    }

    @GetMapping("/batch/{batchId}")
    @Operation(summary = "Get QC checkpoints by batch")
    public ResponseEntity<List<QcCheckpointResponse>> getQcCheckpointsByBatch(@PathVariable String batchId) {
        List<QcCheckpointResponse> responses = qcCheckpointService.getQcCheckpointsByBatch(batchId).stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/batch/{batchId}/mandatory-completed")
    @Operation(summary = "Check if mandatory checkpoints are completed for batch")
    public ResponseEntity<Boolean> hasMandatoryCheckpointsCompleted(@PathVariable String batchId) {
        return ResponseEntity.ok(qcCheckpointService.hasMandatoryCheckpointsCompleted(batchId));
    }

    @GetMapping("/stage/{stage}")
    @Operation(summary = "Get QC checkpoints by stage")
    public ResponseEntity<List<QcCheckpointResponse>> getQcCheckpointsByStage(@PathVariable QcStage stage) {
        List<QcCheckpointResponse> responses = qcCheckpointService.getQcCheckpointsByStage(stage).stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/result/{result}")
    @Operation(summary = "Get QC checkpoints by result")
    public ResponseEntity<List<QcCheckpointResponse>> getQcCheckpointsByResult(@PathVariable QcResult result) {
        List<QcCheckpointResponse> responses = qcCheckpointService.getQcCheckpointsByResult(result).stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/mandatory")
    @Operation(summary = "Get mandatory QC checkpoints")
    public ResponseEntity<List<QcCheckpointResponse>> getMandatoryCheckpoints() {
        List<QcCheckpointResponse> responses = qcCheckpointService.getMandatoryCheckpoints().stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/failed")
    @Operation(summary = "Get failed QC checkpoints")
    public ResponseEntity<List<QcCheckpointResponse>> getFailedCheckpoints() {
        List<QcCheckpointResponse> responses = qcCheckpointService.getFailedCheckpoints().stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get QC checkpoints by date range")
    public ResponseEntity<List<QcCheckpointResponse>> getQcCheckpointsByDateRange(
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {
        List<QcCheckpointResponse> responses = qcCheckpointService
                .getQcCheckpointsByDateRange(startDate, endDate).stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    private void applyUpdate(QcCheckpoint existing, QcCheckpointUpdateRequest request) {
        if (request.getStage() != null) {
            existing.setStage(request.getStage());
        }
        if (request.getResult() != null) {
            existing.setResult(request.getResult());
        }
        if (request.getDefects() != null) {
            existing.setDefects(request.getDefects());
        }
        if (request.getDefectsCount() != null) {
            existing.setDefectsCount(request.getDefectsCount());
        }
        if (request.getInspectorId() != null) {
            existing.setInspector(resolveInspector(request.getInspectorId()));
        }
        if (request.getCheckpointTime() != null) {
            existing.setCheckpointTime(request.getCheckpointTime());
        }
        if (request.getNotes() != null) {
            existing.setNotes(request.getNotes());
        }
    }

    private Operator resolveInspector(String inspectorId) {
        if (inspectorId == null) {
            return null;
        }
        return operatorRepository.findById(inspectorId)
                .orElseThrow(() -> new IllegalArgumentException("Inspector not found with ID: " + inspectorId));
    }
}
