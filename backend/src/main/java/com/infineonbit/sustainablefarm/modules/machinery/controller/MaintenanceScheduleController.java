package com.infineonbit.sustainablefarm.modules.machinery.controller;

import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.MaintenanceScheduleCreationRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.MaintenanceScheduleUpdateRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Response.MaintenanceScheduleObtainingResponse;
import com.infineonbit.sustainablefarm.modules.machinery.service.MaintenanceScheduleService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/machinery/maintenance-schedules")
@AllArgsConstructor
public class MaintenanceScheduleController {
    private final MaintenanceScheduleService maintenanceScheduleService;

    @PostMapping
    public ResponseEntity<MaintenanceScheduleObtainingResponse> createMaintenanceSchedule(
            @Valid @RequestBody MaintenanceScheduleCreationRequest request) {
        MaintenanceScheduleObtainingResponse response = maintenanceScheduleService.addMaintenanceSchedule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<MaintenanceScheduleObtainingResponse>> getAllMaintenanceSchedules(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        Sort sort = sortDirection.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(maintenanceScheduleService.obtainAllMaintenanceSchedules(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceScheduleObtainingResponse> getMaintenanceScheduleById(@PathVariable Long id) {
        return ResponseEntity.ok(maintenanceScheduleService.obtainMaintenanceScheduleById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaintenanceScheduleObtainingResponse> updateMaintenanceSchedule(
            @PathVariable Long id,
            @Valid @RequestBody MaintenanceScheduleUpdateRequest request) {
        return ResponseEntity.ok(maintenanceScheduleService.updateMaintenanceSchedule(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMaintenanceSchedule(@PathVariable Long id) {
        maintenanceScheduleService.deleteMaintenanceSchedule(id);
        return ResponseEntity.noContent().build();
    }
}
