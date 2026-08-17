package com.sustainablefarm.controller;

import com.sustainablefarm.dto.mapper.DtoMapper;
import com.sustainablefarm.dto.request.OperatorCertificationRequest;
import com.sustainablefarm.dto.request.OperatorCreateRequest;
import com.sustainablefarm.dto.request.OperatorStatusUpdateRequest;
import com.sustainablefarm.dto.request.OperatorUpdateRequest;
import com.sustainablefarm.dto.response.OperatorResponse;
import com.sustainablefarm.dto.response.PageResponse;
import com.sustainablefarm.model.Operator;
import com.sustainablefarm.model.Operator.ActiveStatus;
import com.sustainablefarm.model.Operator.Role;
import com.sustainablefarm.service.OperatorService;
import com.sustainablefarm.util.PaginationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/operators")
@Tag(name = "Operator Management", description = "APIs for personnel and role management")
public class OperatorController {

    private final OperatorService operatorService;
    private final DtoMapper dtoMapper;

    @Autowired
    public OperatorController(OperatorService operatorService, DtoMapper dtoMapper) {
        this.operatorService = operatorService;
        this.dtoMapper = dtoMapper;
    }

    @PostMapping
    @Operation(summary = "Create operator")
    public ResponseEntity<OperatorResponse> createOperator(@Valid @RequestBody OperatorCreateRequest request) {
        Operator operator = dtoMapper.toEntity(request);
        Operator created = operatorService.createOperator(operator);
        return ResponseEntity.status(HttpStatus.CREATED).body(dtoMapper.toResponse(created));
    }

    @GetMapping("/{operatorId}")
    @Operation(summary = "Get operator by ID")
    public ResponseEntity<OperatorResponse> getOperatorById(@PathVariable String operatorId) {
        return ResponseEntity.ok(dtoMapper.toResponse(operatorService.getOperatorById(operatorId)));
    }

    @GetMapping
    @Operation(summary = "Get all operators (paginated)")
    public ResponseEntity<PageResponse<OperatorResponse>> getAllOperators(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<OperatorResponse> responses = operatorService.getAllOperators().stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(PaginationUtils.paginate(responses, page, size));
    }

    @PutMapping("/{operatorId}")
    @Operation(summary = "Update operator")
    public ResponseEntity<OperatorResponse> updateOperator(
            @PathVariable String operatorId,
            @Valid @RequestBody OperatorUpdateRequest request) {
        Operator existing = operatorService.getOperatorById(operatorId);
        applyUpdate(existing, request);
        Operator updated = operatorService.updateOperator(operatorId, existing);
        return ResponseEntity.ok(dtoMapper.toResponse(updated));
    }

    @DeleteMapping("/{operatorId}")
    @Operation(summary = "Delete operator")
    public ResponseEntity<Void> deleteOperator(@PathVariable String operatorId) {
        operatorService.deleteOperator(operatorId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{operatorId}/status")
    @Operation(summary = "Update operator active status")
    public ResponseEntity<OperatorResponse> updateActiveStatus(
            @PathVariable String operatorId,
            @Valid @RequestBody OperatorStatusUpdateRequest request) {
        Operator operator = operatorService.updateActiveStatus(operatorId, request.getActiveStatus());
        return ResponseEntity.ok(dtoMapper.toResponse(operator));
    }

    @PostMapping("/{operatorId}/certifications")
    @Operation(summary = "Add certification to operator")
    public ResponseEntity<OperatorResponse> addCertification(
            @PathVariable String operatorId,
            @Valid @RequestBody OperatorCertificationRequest request) {
        Operator operator = operatorService.addCertification(operatorId, request.getCertification());
        return ResponseEntity.ok(dtoMapper.toResponse(operator));
    }

    @GetMapping("/role/{role}")
    @Operation(summary = "Get operators by role")
    public ResponseEntity<List<OperatorResponse>> getOperatorsByRole(@PathVariable Role role) {
        List<OperatorResponse> responses = operatorService.getOperatorsByRole(role).stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get operators by active status")
    public ResponseEntity<List<OperatorResponse>> getOperatorsByStatus(@PathVariable ActiveStatus status) {
        List<OperatorResponse> responses = operatorService.getOperatorsByStatus(status).stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/active/role/{role}")
    @Operation(summary = "Get active operators by role")
    public ResponseEntity<List<OperatorResponse>> getActiveOperatorsByRole(@PathVariable Role role) {
        List<OperatorResponse> responses = operatorService.getActiveOperatorsByRole(role).stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/active/qc-inspectors")
    @Operation(summary = "Get active QC inspectors")
    public ResponseEntity<List<OperatorResponse>> getActiveQcInspectors() {
        List<OperatorResponse> responses = operatorService.getActiveQcInspectors().stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/active/auditors")
    @Operation(summary = "Get active auditors")
    public ResponseEntity<List<OperatorResponse>> getActiveAuditors() {
        List<OperatorResponse> responses = operatorService.getActiveAuditors().stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{operatorId}/active")
    @Operation(summary = "Check if operator is active")
    public ResponseEntity<Boolean> isOperatorActive(@PathVariable String operatorId) {
        return ResponseEntity.ok(operatorService.isOperatorActive(operatorId));
    }

    @GetMapping("/certification/{certification}")
    @Operation(summary = "Get operators by certification")
    public ResponseEntity<List<OperatorResponse>> getOperatorsByCertification(
            @PathVariable String certification) {
        List<OperatorResponse> responses = operatorService.getOperatorsByCertification(certification).stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    private void applyUpdate(Operator existing, OperatorUpdateRequest request) {
        if (request.getOperatorName() != null) {
            existing.setOperatorName(request.getOperatorName());
        }
        if (request.getRole() != null) {
            existing.setRole(request.getRole());
        }
        if (request.getCertifications() != null) {
            existing.setCertifications(request.getCertifications());
        }
        if (request.getActiveStatus() != null) {
            existing.setActiveStatus(request.getActiveStatus());
        }
        if (request.getHireDate() != null) {
            existing.setHireDate(request.getHireDate());
        }
    }
}
