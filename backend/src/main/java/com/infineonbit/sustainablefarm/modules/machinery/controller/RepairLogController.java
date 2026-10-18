package com.infineonbit.sustainablefarm.modules.machinery.controller;

import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.RepairLogCreationRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.RepairLogUpdateRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Response.RepairLogObtainingResponse;
import com.infineonbit.sustainablefarm.modules.machinery.service.RepairLogService;
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
@RequestMapping("/api/machinery/repair-logs")
@AllArgsConstructor
public class RepairLogController {
    private final RepairLogService repairLogService;

    @PostMapping
    public ResponseEntity<RepairLogObtainingResponse> createRepairLog(@Valid @RequestBody RepairLogCreationRequest request) {
        RepairLogObtainingResponse response = repairLogService.addRepairLog(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<RepairLogObtainingResponse>> getAllRepairLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        Sort sort = sortDirection.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(repairLogService.obtainAllRepairLogs(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RepairLogObtainingResponse> getRepairLogById(@PathVariable Long id) {
        return ResponseEntity.ok(repairLogService.obtainRepairLogById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RepairLogObtainingResponse> updateRepairLog(
            @PathVariable Long id,
            @Valid @RequestBody RepairLogUpdateRequest request) {
        return ResponseEntity.ok(repairLogService.updateRepairLog(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRepairLog(@PathVariable Long id) {
        repairLogService.deleteRepairLog(id);
        return ResponseEntity.noContent().build();
    }
}
