package com.infineonbit.sustainablefarm.modules.producttransformation.equipment.controller;

import com.infineonbit.sustainablefarm.modules.producttransformation.dashboard.service.DtoMapper;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.dto.request.EquipmentUpdateRequest;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.dto.response.EquipmentResponse;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.Equipment;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.service.EquipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for Equipment operations: the processing and drying machines of the farm
 * equipment register owned by machinery, with their product transformation attributes
 * (rules in {@code EquipmentRegisterMapping}).
 *
 * <p>Machinery owns the machines: it alone creates, renames, changes the status of and deletes
 * them ({@code /api/machinery/equipment}). This route lists them and edits only their product
 * transformation attributes ({@code pt_equipment_spec}); it has no create and no delete.</p>
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
// Explicit bean name: Machinery also ships an EquipmentController, and two
// @RestController classes with the same simple name make the Spring context
// fail (ConflictingBeanDefinitionException).
@RestController("prodTransEquipmentController")
@RequestMapping("/api/product-transformation/equipment")
@Tag(name = "Equipment Management", description = "Processing and drying machines of machinery, with their product transformation attributes")
public class EquipmentController {

    private final EquipmentService equipmentService;
    private final DtoMapper dtoMapper;

    @Autowired
    public EquipmentController(EquipmentService equipmentService, DtoMapper dtoMapper) {
        this.equipmentService = equipmentService;
        this.dtoMapper = dtoMapper;
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
    @Operation(summary = "Update the product transformation attributes of a machine",
            description = "Updates type, capacity, energy consumption, location and last maintenance date; "
                    + "the machine itself (name, status) is changed in machinery")
    public ResponseEntity<EquipmentResponse> updateEquipment(
            @Parameter(description = "Equipment ID") @PathVariable String equipmentId,
            @Valid @RequestBody EquipmentUpdateRequest request) {
        Equipment spec = equipmentService.getEquipmentById(equipmentId);

        if (request.getEquipmentType() != null) {
            spec.setEquipmentType(request.getEquipmentType());
        }
        if (request.getCapacityKgPerHour() != null) {
            spec.setCapacityKgPerHour(request.getCapacityKgPerHour());
        }
        if (request.getEnergyConsumptionKwhPerKg() != null) {
            spec.setEnergyConsumptionKwhPerKg(request.getEnergyConsumptionKwhPerKg());
        }
        if (request.getLocation() != null) {
            spec.setLocation(request.getLocation());
        }
        if (request.getLastMaintenanceDate() != null) {
            spec.setLastMaintenanceDate(request.getLastMaintenanceDate());
        }

        Equipment updatedEquipment = equipmentService.updateSpec(equipmentId, spec);
        EquipmentResponse response = dtoMapper.toResponse(updatedEquipment);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/type/{equipmentType}")
    @Operation(summary = "Get equipment by type", description = "Retrieves equipment filtered by type")
    public ResponseEntity<List<EquipmentResponse>> getEquipmentByType(
            @Parameter(description = "Equipment type") @PathVariable com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.EquipmentType equipmentType) {
        List<Equipment> equipmentList = equipmentService.getEquipmentByType(equipmentType);
        List<EquipmentResponse> responses = equipmentList.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/status/{maintenanceStatus}")
    @Operation(summary = "Get equipment by maintenance status", description = "Retrieves equipment filtered by maintenance status")
    public ResponseEntity<List<EquipmentResponse>> getEquipmentByStatus(
            @Parameter(description = "Maintenance status") @PathVariable com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.MaintenanceStatus maintenanceStatus) {
        List<Equipment> equipmentList = equipmentService.getEquipmentByStatus(maintenanceStatus);
        List<EquipmentResponse> responses = equipmentList.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/available")
    @Operation(summary = "Get available equipment", description = "Retrieves equipment with ACTIVE maintenance status")
    public ResponseEntity<List<EquipmentResponse>> getAvailableEquipment() {
        List<Equipment> equipmentList = equipmentService.getEquipmentByStatus(com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.MaintenanceStatus.ACTIVE);
        List<EquipmentResponse> responses = equipmentList.stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}
