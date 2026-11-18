package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.DripMaintenanceLog;
import com.infineonbit.sustainablefarm.modules.farm.api.ZoneDto;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.DripMaintenanceLogRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.DripMaintenanceLogResponse;
import com.infineonbit.sustainablefarm.core.web.PageResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.DripMaintenanceLogRepository;
import com.infineonbit.sustainablefarm.modules.farm.api.FarmDirectory;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DripMaintenanceService {
    private final DripMaintenanceLogRepository logRepository;
    private final FarmDirectory farmDirectory;
    private final PersonDirectory personDirectory;

    public DripMaintenanceService(DripMaintenanceLogRepository logRepository, FarmDirectory farmDirectory,
                                  PersonDirectory personDirectory) {
        this.logRepository = logRepository;
        this.farmDirectory = farmDirectory;
        this.personDirectory = personDirectory;
    }

    /** Recurring drip tasks and their periodicity. */
    private static final List<PreventiveTask> PREVENTIVE_TASKS = List.of(
            new PreventiveTask("inspection", 7, "weekly"),
            new PreventiveTask("filter_cleaning", 14, "biweekly"),
            new PreventiveTask("flush", 30, "monthly"));

    /** Delay (days) below which a task is announced as soon due. */
    private static final long DUE_SOON_DAYS = 2;

    /** Preventive task: type stored in the history, periodicity and frequency label. */
    public record PreventiveTask(String type, int intervalDays, String frequency) {
    }

    public List<DripMaintenanceLogResponse> findAll() {
        return responses(logRepository.findAll());
    }

    /** Paginated list, with optional zone filter: same contract as the other module lists. */
    public PageResponse<DripMaintenanceLogResponse> findAll(Pageable pageable, Long zoneId) {
        Page<DripMaintenanceLog> page = zoneId == null
                ? logRepository.findAll(pageable)
                : logRepository.findByZoneId(zoneId, pageable);
        return new PageResponse<>(responses(page.getContent()),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public DripMaintenanceLogResponse create(DripMaintenanceLogRequest request) {
        if (request.zoneId() == null) {
            // Required field missing: 400 (bad payload) and not 404 (unknown zone).
            throw new IllegalArgumentException("Zone is required");
        }
        DripMaintenanceLog log = new DripMaintenanceLog();
        log.setZoneId(request.zoneId());
        log.setMaintenanceDate(request.maintenanceDate());
        log.setMaintenanceType(request.maintenanceType());
        log.setFilterCleaned(request.filterCleaned());
        log.setCloggingDetected(request.cloggingDetected());
        log.setCloggingSeverity(request.cloggingSeverity());
        log.setEmitterReplacedCount(request.emitterReplacedCount());
        log.setNotes(request.notes());
        requireZone(log.getZoneId());
        requireMaintenanceInfo(log.getMaintenanceDate(), log.getMaintenanceType());
        log.setPerformedByPersonId(personDirectory.idOf(request.performedBy()));
        return response(logRepository.save(log));
    }

    public DripMaintenanceLogResponse get(UUID logId) {
        return response(getEntity(logId));
    }

    @Transactional
    public DripMaintenanceLogResponse update(UUID logId, DripMaintenanceLogRequest request) {
        DripMaintenanceLog log = getEntity(logId);
        if (request.zoneId() != null && !request.zoneId().equals(log.getZoneId())) {
            requireZone(request.zoneId());
            log.setZoneId(request.zoneId());
        }
        log.setMaintenanceDate(request.maintenanceDate() == null ? log.getMaintenanceDate() : request.maintenanceDate());
        log.setMaintenanceType(request.maintenanceType() == null ? log.getMaintenanceType() : request.maintenanceType());
        log.setFilterCleaned(request.filterCleaned());
        log.setCloggingDetected(request.cloggingDetected());
        log.setCloggingSeverity(request.cloggingSeverity());
        log.setEmitterReplacedCount(request.emitterReplacedCount());
        log.setNotes(request.notes());
        requireMaintenanceInfo(log.getMaintenanceDate(), log.getMaintenanceType());
        log.setPerformedByPersonId(personDirectory.idOf(request.performedBy()));
        return response(logRepository.save(log));
    }

    @Transactional
    public void delete(UUID logId) {
        logRepository.delete(getEntity(logId));
    }

    /** API view; performedBy is the full name of the referenced person. */
    private DripMaintenanceLogResponse response(DripMaintenanceLog log) {
        return DripMaintenanceLogResponse.from(log, personDirectory.nameOf(log.getPerformedByPersonId()));
    }

    /** API views of several logs, with one lookup of the people. */
    private List<DripMaintenanceLogResponse> responses(List<DripMaintenanceLog> logs) {
        Map<Long, String> names = personDirectory.namesOf(
                logs.stream().map(DripMaintenanceLog::getPerformedByPersonId).toList());
        return logs.stream()
                .map(log -> DripMaintenanceLogResponse.from(log, names.get(log.getPerformedByPersonId())))
                .toList();
    }

    private DripMaintenanceLog getEntity(UUID logId) {
        return logRepository.findById(logId)
                .orElseThrow(() -> new NotFoundException("DripMaintenanceLog"));
    }

    /** An intervention must carry a date and a type: the schema declares them required. */
    private void requireMaintenanceInfo(java.time.Instant maintenanceDate, String maintenanceType) {
        if (maintenanceDate == null) {
            throw new IllegalArgumentException("Maintenance date is required");
        }
        if (maintenanceType == null || maintenanceType.isBlank()) {
            throw new IllegalArgumentException("Maintenance type is required");
        }
    }

    /**
     * Preventive maintenance calendar (specification module 6.3).
     *
     * <p>Each recurring task is due per zone based on the last intervention recorded for that
     * type: weekly inspection, biweekly filter cleaning, monthly flush. The calendar is therefore
     * no longer a fixed list but a deadline per zone, with any delay — this is what allows acting
     * before water loss.</p>
     */
    public List<Map<String, Object>> schedule() {
        List<DripMaintenanceLog> logs = logRepository.findAll();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<Map<String, Object>> calendar = new ArrayList<>();
        for (ZoneDto zone : farmDirectory.listZones()) {
            for (PreventiveTask task : PREVENTIVE_TASKS) {
                calendar.add(taskEntry(zone, task, lastDone(logs, zone.id(), task.type()), today));
            }
        }
        return calendar;
    }

    /** Last intervention of a given type for a zone, or null if it never happened. */
    private LocalDate lastDone(List<DripMaintenanceLog> logs, Long zoneId, String taskType) {
        LocalDate last = null;
        for (DripMaintenanceLog log : logs) {
            if (log.getMaintenanceDate() == null || !zoneId.equals(log.getZoneId())
                    || !matchesTask(log.getMaintenanceType(), taskType)) {
                continue;
            }
            LocalDate date = LocalDate.ofInstant(log.getMaintenanceDate(), ZoneOffset.UTC);
            if (last == null || date.isAfter(last)) {
                last = date;
            }
        }
        return last;
    }

    /** Tolerates manually typed labels ("filter cleaning", "inspection"...). */
    private boolean matchesTask(String maintenanceType, String taskType) {
        String value = maintenanceType == null ? ""
                : maintenanceType.trim().toLowerCase().replace(' ', '_').replace('-', '_');
        return switch (taskType) {
            case "filter_cleaning" -> value.contains("filter");
            case "flush" -> value.contains("flush");
            default -> value.contains("inspection") || value.contains("inspect") || value.contains("visit");
        };
    }

    private Map<String, Object> taskEntry(ZoneDto zone, PreventiveTask task, LocalDate lastDone, LocalDate today) {
        LocalDate nextDue = lastDone == null ? today : lastDone.plusDays(task.intervalDays());
        long daysRemaining = ChronoUnit.DAYS.between(today, nextDue);
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("zone_id", zone.id());
        entry.put("zone_name", zone.name());
        entry.put("task_type", task.type());
        entry.put("frequency", task.frequency());
        entry.put("interval_days", task.intervalDays());
        entry.put("last_done", lastDone == null ? null : lastDone.toString());
        entry.put("next_due", nextDue.toString());
        entry.put("days_remaining", daysRemaining);
        entry.put("status", status(lastDone, daysRemaining));
        return entry;
    }

    private String status(LocalDate lastDone, long daysRemaining) {
        if (lastDone == null) {
            return "never_done";
        }
        if (daysRemaining < 0) {
            return "overdue";
        }
        return daysRemaining <= DUE_SOON_DAYS ? "due_soon" : "planned";
    }

    private void requireZone(Long zoneId) {
        if (zoneId == null || farmDirectory.findZone(zoneId).isEmpty()) {
            throw new NotFoundException("Zone");
        }
    }
}