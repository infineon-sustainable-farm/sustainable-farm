package com.infineonbit.sustainablefarm.modules.machinery.controller;

import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.UsageLogCreationRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.UsageLogUpdateRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Response.UsageLogObtainingResponse;
import com.infineonbit.sustainablefarm.modules.machinery.service.UsageLogService;
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
@RequestMapping("/api/machinery/usage-logs")
@AllArgsConstructor
public class UsageLogController {
    private final UsageLogService usageLogService;

    @PostMapping
    public ResponseEntity<UsageLogObtainingResponse> createUsageLog(@Valid @RequestBody UsageLogCreationRequest request) {
        UsageLogObtainingResponse response = usageLogService.addUsageLog(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<UsageLogObtainingResponse>> getAllUsageLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        Sort sort = sortDirection.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(usageLogService.obtainAllUsageLogs(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsageLogObtainingResponse> getUsageLogById(@PathVariable Long id) {
        return ResponseEntity.ok(usageLogService.obtainUsageLogById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsageLogObtainingResponse> updateUsageLog(
            @PathVariable Long id,
            @Valid @RequestBody UsageLogUpdateRequest request) {
        return ResponseEntity.ok(usageLogService.updateUsageLog(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUsageLog(@PathVariable Long id) {
        usageLogService.deleteUsageLog(id);
        return ResponseEntity.noContent().build();
    }
}
