package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.DripMaintenanceLog;
import com.infineonbit.sustainablefarm.modules.watersupply.service.DripMaintenanceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public List<DripMaintenanceLog> list() {
        return maintenanceService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DripMaintenanceLog create(@Valid @RequestBody DripMaintenanceLog log) {
        return maintenanceService.create(log);
    }

    @GetMapping("/{logId}")
    public DripMaintenanceLog get(@PathVariable UUID logId) {
        return maintenanceService.get(logId);
    }

    @PutMapping("/{logId}")
    public DripMaintenanceLog update(@PathVariable UUID logId, @RequestBody DripMaintenanceLog payload) {
        return maintenanceService.update(logId, payload);
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
