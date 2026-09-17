package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.DripMaintenanceLog;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.DripMaintenanceLogRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.ZoneRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DripMaintenanceService {
    private final DripMaintenanceLogRepository logRepository;
    private final ZoneRepository zoneRepository;

    public DripMaintenanceService(DripMaintenanceLogRepository logRepository, ZoneRepository zoneRepository) {
        this.logRepository = logRepository;
        this.zoneRepository = zoneRepository;
    }

    public List<DripMaintenanceLog> findAll() {
        return logRepository.findAll();
    }

    @Transactional
    public DripMaintenanceLog create(DripMaintenanceLog log) {
        requireZone(log.getZoneId());
        return logRepository.save(log);
    }

    public DripMaintenanceLog get(UUID logId) {
        return logRepository.findById(logId)
                .orElseThrow(() -> new NotFoundException("DripMaintenanceLog"));
    }

    @Transactional
    public DripMaintenanceLog update(UUID logId, DripMaintenanceLog payload) {
        DripMaintenanceLog log = get(logId);
        if (payload.getZoneId() != null && !payload.getZoneId().equals(log.getZoneId())) {
            requireZone(payload.getZoneId());
            log.setZoneId(payload.getZoneId());
        }
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

    @Transactional
    public void delete(UUID logId) {
        logRepository.delete(get(logId));
    }

    public List<Map<String, Object>> schedule() {
        return List.of(
                Map.of("task_type", "inspection", "frequency", "weekly"),
                Map.of("task_type", "filter_cleaning", "frequency", "biweekly"),
                Map.of("task_type", "flush", "frequency", "monthly"));
    }

    private void requireZone(UUID zoneId) {
        if (zoneId == null || zoneRepository.findById(zoneId).isEmpty()) {
            throw new NotFoundException("Zone");
        }
    }
}