package com.infineonbit.sustainablefarm.modules.machinery.controller;

import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.FuelLogCreationRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.FuelLogUpdateRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Response.FuelLogObtainingResponse;
import com.infineonbit.sustainablefarm.modules.machinery.service.FuelLogService;
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
@RequestMapping("/api/machinery/fuel-logs")
@AllArgsConstructor
public class FuelLogController {
    private final FuelLogService fuelLogService;

    @PostMapping
    public ResponseEntity<FuelLogObtainingResponse> createFuelLog(@Valid @RequestBody FuelLogCreationRequest request) {
        FuelLogObtainingResponse response = fuelLogService.addFuelLog(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<FuelLogObtainingResponse>> getAllFuelLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        Sort sort = sortDirection.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(fuelLogService.obtainAllFuelLogs(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FuelLogObtainingResponse> getFuelLogById(@PathVariable Long id) {
        return ResponseEntity.ok(fuelLogService.obtainFuelLogById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FuelLogObtainingResponse> updateFuelLog(
            @PathVariable Long id,
            @Valid @RequestBody FuelLogUpdateRequest request) {
        return ResponseEntity.ok(fuelLogService.updateFuelLog(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFuelLog(@PathVariable Long id) {
        fuelLogService.deleteFuelLog(id);
        return ResponseEntity.noContent().build();
    }
}
