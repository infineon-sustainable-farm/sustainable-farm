package com.sustainablefarm.controller;

import com.sustainablefarm.core.dto.mapper.DtoMapper;
import com.sustainablefarm.dto.request.EquipmentCreateRequest;
import com.sustainablefarm.dto.request.EquipmentUpdateRequest;
import com.sustainablefarm.dto.response.EquipmentResponse;
import com.sustainablefarm.model.Equipment;
import com.sustainablefarm.service.EquipmentService;
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
 * REST Controller for Equipment operations
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/equipment")
@Tag(name = "Equipment Management", description = "APIs for managing equipment and machinery")
public class EquipmentController {

    private final EquipmentService equipmentService;
    private final DtoMapper dtoMapper;

    @Autowired
    public EquipmentController(EquipmentService equipmentService, DtoMapper dtoMapper) {
        this.equipmentService = equipmentService;
        this.dtoMapper = dtoMapper;
    }

    @PostMapping
    @Operation(summary = "Create new equipment", description = "Creates a new equipment record")
    public ResponseEntity<EquipmentResponse> createEquipment(@Valid @RequestBody EquipmentCreateRequest request) {
        Equipment equipment = dtoMapper.toEntity(request);
        Equipment createdEquipment = equipmentService.createEquipment(equipment);
        EquipmentResponse response = dtoMapper.toResponse(createdEquipment);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{equipmentId}")
    @Operation(summary = "Get equipment by ID", description = "Retrieves specific equipment by its ID")
    public ResponseEntity<EquipmentResponse> getEquipmentById(
            @Parameter(description = "Equipment ID") @PathVariable String equipmentId) {
        Equipment equipment = equipmentService.getEquipmentById(equipmentId);
        EquipmentResponse response = dtoMapper.toResponse(equipment);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all equipment", description = "Retrieves all equipment records")
    public ResponseEntity<List<EquipmentResponse>> getAllEquipment() {
        List<Equipment> equipmentList = equipmentService.getAllEquipment();
        List<EquipmentResponse> responses = equipmentList.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{equipmentId}")
    @Operation(summary = "Update equipment", description = "Updates an existing equipment record")
    public ResponseEntity<EquipmentResponse> updateEquipment(
            @Parameter(description = "Equipment ID") @PathVariable String equipmentId,
            @Valid @RequestBody EquipmentUpdateRequest request) {
        Equipment existingEquipment = equipmentService.getEquipmentById(equipmentId);
        
        if (request.getEquipmentName() != null) {
            existingEquipment.setEquipmentName(request.getEquipmentName());
        }
        if (request.getEquipmentType() != null) {
            existingEquipment.setEquipmentType(request.getEquipmentType());
        }
        if (request.getCapacityKgPerHour() != null) {
            existingEquipment.setCapacityKgPerHour(request.getCapacityKgPerHour());
        }
        if (request.getEnergyConsumptionKwhPerKg() != null) {
            existingEquipment.setEnergyConsumptionKwhPerKg(request.getEnergyConsumptionKwhPerKg());
        }
        if (request.getLocation() != null) {
            existingEquipment.setLocation(request.getLocation());
        }
        if (request.getMaintenanceStatus() != null) {
            existingEquipment.setMaintenanceStatus(request.getMaintenanceStatus());
        }
        if (request.getLastMaintenanceDate() != null) {
            existingEquipment.setLastMaintenanceDate(request.getLastMaintenanceDate());
        }
        
        Equipment updatedEquipment = equipmentService.updateEquipment(equipmentId, existingEquipment);
        EquipmentResponse response = dtoMapper.toResponse(updatedEquipment);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{equipmentId}")
    @Operation(summary = "Delete equipment", description = "Deletes an equipment record by its ID")
    public ResponseEntity<Void> deleteEquipment(
            @Parameter(description = "Equipment ID") @PathVariable String equipmentId) {
        equipmentService.deleteEquipment(equipmentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/type/{equipmentType}")
    @Operation(summary = "Get equipment by type", description = "Retrieves equipment filtered by type")
    public ResponseEntity<List<EquipmentResponse>> getEquipmentByType(
            @Parameter(description = "Equipment type") @PathVariable com.sustainablefarm.model.Equipment.EquipmentType equipmentType) {
        List<Equipment> equipmentList = equipmentService.getEquipmentByType(equipmentType);
        List<EquipmentResponse> responses = equipmentList.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/status/{maintenanceStatus}")
    @Operation(summary = "Get equipment by maintenance status", description = "Retrieves equipment filtered by maintenance status")
    public ResponseEntity<List<EquipmentResponse>> getEquipmentByStatus(
            @Parameter(description = "Maintenance status") @PathVariable com.sustainablefarm.model.Equipment.MaintenanceStatus maintenanceStatus) {
        List<Equipment> equipmentList = equipmentService.getEquipmentByStatus(maintenanceStatus);
        List<EquipmentResponse> responses = equipmentList.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/available")
    @Operation(summary = "Get available equipment", description = "Retrieves equipment with ACTIVE maintenance status")
    public ResponseEntity<List<EquipmentResponse>> getAvailableEquipment() {
        List<Equipment> equipmentList = equipmentService.getEquipmentByStatus(com.sustainablefarm.model.Equipment.MaintenanceStatus.ACTIVE);
        List<EquipmentResponse> responses = equipmentList.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}
