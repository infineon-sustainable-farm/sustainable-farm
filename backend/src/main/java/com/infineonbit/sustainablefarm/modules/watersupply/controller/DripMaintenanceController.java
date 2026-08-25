package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.DripMaintenanceLog;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.DripMaintenanceLogRepository;
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
    private final DripMaintenanceLogRepository logRepository;

    public DripMaintenanceController(DripMaintenanceLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @GetMapping
    public List<DripMaintenanceLog> list() {
        return logRepository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DripMaintenanceLog create(@Valid @RequestBody DripMaintenanceLog log) {
        return logRepository.save(log);
    }

    @GetMapping("/{logId}")
    public DripMaintenanceLog get(@PathVariable UUID logId) {
        return logRepository.findById(logId).orElseThrow(() -> new NotFoundException("DripMaintenanceLog"));
    }

    @PutMapping("/{logId}")
    public DripMaintenanceLog update(@PathVariable UUID logId, @RequestBody DripMaintenanceLog payload) {
        DripMaintenanceLog log = logRepository.findById(logId)
                .orElseThrow(() -> new NotFoundException("DripMaintenanceLog"));
        log.setZoneId(payload.getZoneId() == null ? log.getZoneId() : payload.getZoneId());
        log.setMaintenanceDate(payload.getMaintenanceDate() == null ? log.getMaintenanceDate() : payload.getMaintenanceDate());
        log.setMaintenanceType(payload.getMaintenanceType() == null ? log.getMaintenanceType() : payload.getMaintenanceType());
        log.setFilterCleaned(payload.getFilterCleaned());
        log.setCloggingDetected(payload.getCloggingDetected());
        log.setCloggingSeverity(payload.getCloggingSeverity());
        log.setEmitterReplacedCount(payload.getEmitterReplacedCount());
        log.setNotes(payload.getNotes());
        log.setPerformedBy(payload.getPerformedBy());
        return logRepository.save(log);
    }

    @DeleteMapping("/{logId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID logId) {
        logRepository.delete(logRepository.findById(logId).orElseThrow(() -> new NotFoundException("DripMaintenanceLog")));
    }

    @GetMapping("/schedule")
    public List<Map<String, Object>> schedule() {
        return List.of(
                Map.of("task_type", "inspection", "frequency", "weekly"),
                Map.of("task_type", "filter_cleaning", "frequency", "biweekly"),
                Map.of("task_type", "flush", "frequency", "monthly"));
    }
}
