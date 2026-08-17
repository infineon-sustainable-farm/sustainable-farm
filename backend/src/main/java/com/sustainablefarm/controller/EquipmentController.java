package com.sustainablefarm.controller;

import com.sustainablefarm.dto.mapper.DtoMapper;
import com.sustainablefarm.dto.request.EquipmentCreateRequest;
import com.sustainablefarm.dto.request.EquipmentMaintenanceStatusRequest;
import com.sustainablefarm.dto.request.EquipmentScheduleMaintenanceRequest;
import com.sustainablefarm.dto.request.EquipmentUpdateRequest;
import com.sustainablefarm.dto.response.EquipmentResponse;
import com.sustainablefarm.dto.response.PageResponse;
import com.sustainablefarm.model.Equipment;
import com.sustainablefarm.model.Equipment.EquipmentType;
import com.sustainablefarm.model.Equipment.MaintenanceStatus;
import com.sustainablefarm.service.EquipmentService;
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
@RequestMapping("/api/equipment")
@Tag(name = "Equipment Management", description = "APIs for equipment registry and maintenance")
public class EquipmentController {

    private final EquipmentService equipmentService;
    private final DtoMapper dtoMapper;

    @Autowired
    public EquipmentController(EquipmentService equipmentService, DtoMapper dtoMapper) {
        this.equipmentService = equipmentService;
        this.dtoMapper = dtoMapper;
    }

    @PostMapping
    @Operation(summary = "Create equipment")
    public ResponseEntity<EquipmentResponse> createEquipment(@Valid @RequestBody EquipmentCreateRequest request) {
        Equipment equipment = dtoMapper.toEntity(request);
        Equipment created = equipmentService.createEquipment(equipment);
        return ResponseEntity.status(HttpStatus.CREATED).body(dtoMapper.toResponse(created));
    }

    @GetMapping("/{equipmentId}")
    @Operation(summary = "Get equipment by ID")
    public ResponseEntity<EquipmentResponse> getEquipmentById(@PathVariable String equipmentId) {
        return ResponseEntity.ok(dtoMapper.toResponse(equipmentService.getEquipmentById(equipmentId)));
    }

    @GetMapping
    @Operation(summary = "Get all equipment (paginated)")
    public ResponseEntity<PageResponse<EquipmentResponse>> getAllEquipment(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<EquipmentResponse> responses = equipmentService.getAllEquipment().stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(PaginationUtils.paginate(responses, page, size));
    }

    @PutMapping("/{equipmentId}")
    @Operation(summary = "Update equipment")
    public ResponseEntity<EquipmentResponse> updateEquipment(
            @PathVariable String equipmentId,
            @Valid @RequestBody EquipmentUpdateRequest request) {
        Equipment existing = equipmentService.getEquipmentById(equipmentId);
        applyUpdate(existing, request);
        Equipment updated = equipmentService.updateEquipment(equipmentId, existing);
        return ResponseEntity.ok(dtoMapper.toResponse(updated));
    }

    @DeleteMapping("/{equipmentId}")
    @Operation(summary = "Delete equipment")
    public ResponseEntity<Void> deleteEquipment(@PathVariable String equipmentId) {
        equipmentService.deleteEquipment(equipmentId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{equipmentId}/maintenance-status")
    @Operation(summary = "Update equipment maintenance status")
    public ResponseEntity<EquipmentResponse> updateMaintenanceStatus(
            @PathVariable String equipmentId,
            @Valid @RequestBody EquipmentMaintenanceStatusRequest request) {
        Equipment equipment = equipmentService.updateMaintenanceStatus(equipmentId, request.getMaintenanceStatus());
        return ResponseEntity.ok(dtoMapper.toResponse(equipment));
    }

    @PostMapping("/{equipmentId}/schedule-maintenance")
    @Operation(summary = "Schedule equipment maintenance")
    public ResponseEntity<EquipmentResponse> scheduleMaintenance(
            @PathVariable String equipmentId,
            @Valid @RequestBody EquipmentScheduleMaintenanceRequest request) {
        Equipment equipment = equipmentService.scheduleMaintenance(equipmentId, request.getMaintenanceDate());
        return ResponseEntity.ok(dtoMapper.toResponse(equipment));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get equipment by type")
    public ResponseEntity<List<EquipmentResponse>> getEquipmentByType(@PathVariable EquipmentType type) {
        List<EquipmentResponse> responses = equipmentService.getEquipmentByType(type).stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get equipment by maintenance status")
    public ResponseEntity<List<EquipmentResponse>> getEquipmentByStatus(@PathVariable MaintenanceStatus status) {
        List<EquipmentResponse> responses = equipmentService.getEquipmentByStatus(status).stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/available/{type}")
    @Operation(summary = "Get available equipment by type")
    public ResponseEntity<List<EquipmentResponse>> getAvailableEquipmentByType(@PathVariable EquipmentType type) {
        List<EquipmentResponse> responses = equipmentService.getAvailableEquipmentByType(type).stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{equipmentId}/available")
    @Operation(summary = "Check if equipment is available")
    public ResponseEntity<Boolean> isEquipmentAvailable(@PathVariable String equipmentId) {
        return ResponseEntity.ok(equipmentService.isEquipmentAvailable(equipmentId));
    }

    @GetMapping("/needing-maintenance")
    @Operation(summary = "Get equipment needing maintenance")
    public ResponseEntity<List<EquipmentResponse>> getEquipmentNeedingMaintenance(
            @RequestParam(required = false) LocalDate date) {
        LocalDate checkDate = date != null ? date : LocalDate.now();
        List<EquipmentResponse> responses = equipmentService.getEquipmentNeedingMaintenance(checkDate).stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    private void applyUpdate(Equipment existing, EquipmentUpdateRequest request) {
        if (request.getEquipmentName() != null) {
            existing.setEquipmentName(request.getEquipmentName());
        }
        if (request.getEquipmentType() != null) {
            existing.setEquipmentType(request.getEquipmentType());
        }
        if (request.getCapacityKgPerHour() != null) {
            existing.setCapacityKgPerHour(request.getCapacityKgPerHour());
        }
        if (request.getEnergyConsumptionKwhPerKg() != null) {
            existing.setEnergyConsumptionKwhPerKg(request.getEnergyConsumptionKwhPerKg());
        }
        if (request.getLocation() != null) {
            existing.setLocation(request.getLocation());
        }
        if (request.getMaintenanceStatus() != null) {
            existing.setMaintenanceStatus(request.getMaintenanceStatus());
        }
        if (request.getLastMaintenanceDate() != null) {
            existing.setLastMaintenanceDate(request.getLastMaintenanceDate());
        }
    }
}
