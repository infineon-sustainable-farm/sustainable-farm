package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.DripMaintenanceLogRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.DripMaintenanceLogResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.service.DripMaintenanceService;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/drip-maintenance-logs")
public class DripMaintenanceController {
    private final DripMaintenanceService maintenanceService;

    public DripMaintenanceController(DripMaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @GetMapping
    public Object list(
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Min(1) Integer size,
            @RequestParam(required = false) UUID zoneId) {
        if (page == null && size == null && zoneId == null) {
            return maintenanceService.findAll();
        }
        return maintenanceService.findAll(
                PageRequest.of(page == null ? 0 : page, size == null ? 20 : size,
                        Sort.by(Sort.Direction.DESC, "maintenanceDate")),
                zoneId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DripMaintenanceLogResponse create(@RequestBody DripMaintenanceLogRequest request) {
        return maintenanceService.create(request);
    }

    @GetMapping("/{logId}")
    public DripMaintenanceLogResponse get(@PathVariable UUID logId) {
        return maintenanceService.get(logId);
    }

    @PutMapping("/{logId}")
    public DripMaintenanceLogResponse update(@PathVariable UUID logId,
            @RequestBody DripMaintenanceLogRequest request) {
        return maintenanceService.update(logId, request);
    }

    @DeleteMapping("/{logId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID logId) {
        maintenanceService.delete(logId);
    }

    @GetMapping("/schedule")
    public List<Map<String, Object>> schedule() {
        return maintenanceService.schedule();
    }
}
