package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.DripMaintenanceLog;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Zone;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.DripMaintenanceLogRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.DripMaintenanceLogResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.PageResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.DripMaintenanceLogRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.ZoneRepository;
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
    private final ZoneRepository zoneRepository;

    public DripMaintenanceService(DripMaintenanceLogRepository logRepository, ZoneRepository zoneRepository) {
        this.logRepository = logRepository;
        this.zoneRepository = zoneRepository;
    }

    /** Taches recurrentes du goutte-a-goutte et leur periodicite. */
    private static final List<PreventiveTask> PREVENTIVE_TASKS = List.of(
            new PreventiveTask("inspection", 7, "weekly"),
            new PreventiveTask("filter_cleaning", 14, "biweekly"),
            new PreventiveTask("flush", 30, "monthly"));

    /** Delai (jours) en dessous duquel une tache est annoncee comme bientot due. */
    private static final long DUE_SOON_DAYS = 2;

    /** Tache preventive : type enregistre dans l'historique, periodicite et libelle de frequence. */
    public record PreventiveTask(String type, int intervalDays, String frequency) {
    }

    public List<DripMaintenanceLogResponse> findAll() {
        return logRepository.findAll().stream().map(DripMaintenanceLogResponse::from).toList();
    }

    /** Liste paginee, avec filtre optionnel par zone : meme contrat que les autres listes du module. */
    public PageResponse<DripMaintenanceLogResponse> findAll(Pageable pageable, UUID zoneId) {
        Page<DripMaintenanceLog> page = zoneId == null
                ? logRepository.findAll(pageable)
                : logRepository.findByZoneId(zoneId, pageable);
        return new PageResponse<>(page.getContent().stream().map(DripMaintenanceLogResponse::from).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public DripMaintenanceLogResponse create(DripMaintenanceLogRequest request) {
        if (request.zoneId() == null) {
            // Champ obligatoire manquant : 400 (mauvais contrat) et non 404 (zone inconnue).
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
        log.setPerformedBy(request.performedBy());
        requireZone(log.getZoneId());
        requireMaintenanceInfo(log.getMaintenanceDate(), log.getMaintenanceType());
        return DripMaintenanceLogResponse.from(logRepository.save(log));
    }

    public DripMaintenanceLogResponse get(UUID logId) {
        return DripMaintenanceLogResponse.from(getEntity(logId));
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
        log.setPerformedBy(request.performedBy());
        requireMaintenanceInfo(log.getMaintenanceDate(), log.getMaintenanceType());
        return DripMaintenanceLogResponse.from(logRepository.save(log));
    }

    @Transactional
    public void delete(UUID logId) {
        logRepository.delete(getEntity(logId));
    }

    private DripMaintenanceLog getEntity(UUID logId) {
        return logRepository.findById(logId)
                .orElseThrow(() -> new NotFoundException("DripMaintenanceLog"));
    }

    /** Une intervention doit porter une date et un type : le schema les declare obligatoires. */
    private void requireMaintenanceInfo(java.time.Instant maintenanceDate, String maintenanceType) {
        if (maintenanceDate == null) {
            throw new IllegalArgumentException("Maintenance date is required");
        }
        if (maintenanceType == null || maintenanceType.isBlank()) {
            throw new IllegalArgumentException("Maintenance type is required");
        }
    }

    /**
     * Calendrier de maintenance preventive (module 6.3 de la specification).
     *
     * <p>Chaque tache recurrente est due par zone a partir de la derniere intervention
     * enregistree pour ce type : inspection hebdomadaire, nettoyage de filtre bimensuel,
     * flush mensuel. Le calendrier n'est donc plus une liste figee mais une echeance par zone,
     * avec le retard eventuel — c'est ce qui permet d'agir avant la perte d'eau.</p>
     */
    public List<Map<String, Object>> schedule() {
        List<DripMaintenanceLog> logs = logRepository.findAll();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<Map<String, Object>> calendar = new ArrayList<>();
        for (Zone zone : zoneRepository.findAll()) {
            for (PreventiveTask task : PREVENTIVE_TASKS) {
                calendar.add(taskEntry(zone, task, lastDone(logs, zone.getId(), task.type()), today));
            }
        }
        return calendar;
    }

    /** Derniere intervention d'un type donne pour une zone, ou null si elle n'a jamais eu lieu. */
    private LocalDate lastDone(List<DripMaintenanceLog> logs, UUID zoneId, String taskType) {
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

    /** Tolere les libelles saisis a la main (« filter cleaning », « inspection »...). */
    private boolean matchesTask(String maintenanceType, String taskType) {
        String value = maintenanceType == null ? ""
                : maintenanceType.trim().toLowerCase().replace(' ', '_').replace('-', '_');
        return switch (taskType) {
            case "filter_cleaning" -> value.contains("filter");
            case "flush" -> value.contains("flush");
            default -> value.contains("inspection") || value.contains("inspect") || value.contains("visit");
        };
    }

    private Map<String, Object> taskEntry(Zone zone, PreventiveTask task, LocalDate lastDone, LocalDate today) {
        LocalDate nextDue = lastDone == null ? today : lastDone.plusDays(task.intervalDays());
        long daysRemaining = ChronoUnit.DAYS.between(today, nextDue);
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("zone_id", zone.getId());
        entry.put("zone_name", zone.getName());
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

    private void requireZone(UUID zoneId) {
        if (zoneId == null || zoneRepository.findById(zoneId).isEmpty()) {
            throw new NotFoundException("Zone");
        }
    }
}