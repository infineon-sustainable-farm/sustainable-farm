package com.sustainablefarm.controller;

import com.sustainablefarm.dto.mapper.DtoMapper;
import com.sustainablefarm.dto.request.OperatorCreateRequest;
import com.sustainablefarm.dto.request.OperatorUpdateRequest;
import com.sustainablefarm.dto.response.OperatorResponse;
import com.sustainablefarm.model.Operator;
import com.sustainablefarm.service.OperatorService;
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
 * REST Controller for Operator operations
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/operators")
@Tag(name = "Operator Management", description = "APIs for managing personnel/operators")
public class OperatorController {

    private final OperatorService operatorService;
    private final DtoMapper dtoMapper;

    @Autowired
    public OperatorController(OperatorService operatorService, DtoMapper dtoMapper) {
        this.operatorService = operatorService;
        this.dtoMapper = dtoMapper;
    }

    @PostMapping
    @Operation(summary = "Create new operator", description = "Creates a new operator record")
    public ResponseEntity<OperatorResponse> createOperator(@Valid @RequestBody OperatorCreateRequest request) {
        Operator operator = dtoMapper.toEntity(request);
        Operator createdOperator = operatorService.createOperator(operator);
        OperatorResponse response = dtoMapper.toResponse(createdOperator);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{operatorId}")
    @Operation(summary = "Get operator by ID", description = "Retrieves specific operator by their ID")
    public ResponseEntity<OperatorResponse> getOperatorById(
            @Parameter(description = "Operator ID") @PathVariable String operatorId) {
        Operator operator = operatorService.getOperatorById(operatorId);
        OperatorResponse response = dtoMapper.toResponse(operator);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all operators", description = "Retrieves all operator records")
    public ResponseEntity<List<OperatorResponse>> getAllOperators() {
        List<Operator> operators = operatorService.getAllOperators();
        List<OperatorResponse> responses = operators.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{operatorId}")
    @Operation(summary = "Update operator", description = "Updates an existing operator record")
    public ResponseEntity<OperatorResponse> updateOperator(
            @Parameter(description = "Operator ID") @PathVariable String operatorId,
            @Valid @RequestBody OperatorUpdateRequest request) {
        Operator existingOperator = operatorService.getOperatorById(operatorId);
        
        if (request.getOperatorName() != null) {
            existingOperator.setOperatorName(request.getOperatorName());
        }
        if (request.getRole() != null) {
            existingOperator.setRole(request.getRole());
        }
        if (request.getCertifications() != null) {
            existingOperator.setCertifications(request.getCertifications());
        }
        if (request.getActiveStatus() != null) {
            existingOperator.setActiveStatus(request.getActiveStatus());
        }
        if (request.getHireDate() != null) {
            existingOperator.setHireDate(request.getHireDate());
        }
        
        Operator updatedOperator = operatorService.updateOperator(operatorId, existingOperator);
        OperatorResponse response = dtoMapper.toResponse(updatedOperator);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{operatorId}")
    @Operation(summary = "Delete operator", description = "Deletes an operator record by their ID")
    public ResponseEntity<Void> deleteOperator(
            @Parameter(description = "Operator ID") @PathVariable String operatorId) {
        operatorService.deleteOperator(operatorId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/role/{role}")
    @Operation(summary = "Get operators by role", description = "Retrieves operators filtered by role")
    public ResponseEntity<List<OperatorResponse>> getOperatorsByRole(
            @Parameter(description = "Operator role") @PathVariable com.sustainablefarm.model.Operator.Role role) {
        List<Operator> operators = operatorService.getOperatorsByRole(role);
        List<OperatorResponse> responses = operators.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/status/{activeStatus}")
    @Operation(summary = "Get operators by status", description = "Retrieves operators filtered by active status")
    public ResponseEntity<List<OperatorResponse>> getOperatorsByStatus(
            @Parameter(description = "Active status") @PathVariable com.sustainablefarm.model.Operator.ActiveStatus activeStatus) {
        List<Operator> operators = operatorService.getOperatorsByStatus(activeStatus);
        List<OperatorResponse> responses = operators.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/active")
    @Operation(summary = "Get active operators", description = "Retrieves operators with ACTIVE status")
    public ResponseEntity<List<OperatorResponse>> getActiveOperators() {
        List<Operator> operators = operatorService.getOperatorsByStatus(com.sustainablefarm.model.Operator.ActiveStatus.ACTIVE);
        List<OperatorResponse> responses = operators.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/certified/{role}")
    @Operation(summary = "Get certified operators by role", description = "Retrieves active operators with specific role")
    public ResponseEntity<List<OperatorResponse>> getCertifiedOperatorsByRole(
            @Parameter(description = "Operator role") @PathVariable com.sustainablefarm.model.Operator.Role role) {
        List<Operator> operators = operatorService.getActiveOperatorsByRole(role);
        List<OperatorResponse> responses = operators.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}
