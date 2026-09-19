package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.config.SystemUsers;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationLog;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationSchedule;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Zone;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.IrrigationLogCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.IrrigationLogResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.IrrigationLogUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.IrrigationScheduleCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.IrrigationScheduleResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.IrrigationScheduleUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.PageResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IrrigationLogRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IrrigationScheduleRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.ZoneRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class IrrigationService {
    /**
     * Identifiant systeme utilise comme createdBy quand aucun contexte utilisateur n'est fourni
     * (voir {@link SystemUsers} : l'authentification est prise en charge par le logiciel global).
     */
    private static final UUID DEFAULT_SYSTEM_USER_ID = SystemUsers.IOT_SYSTEM_USER_ID;

    /**
     * Seuil de probabilite de pluie (%) a partir duquel une irrigation planifiee est deconseillee.
     * Valeur retenue apres validation fonctionnelle : en dessous, la pluie annoncee n'est pas
     * suffisamment fiable pour justifier un report.
     */
    public static final int RAIN_PROBABILITY_THRESHOLD = 60;

    /** Quantite de pluie prevue (mm) a partir de laquelle le report est justifie. */
    public static final double RAIN_AMOUNT_THRESHOLD_MM = 5.0;

    private final IrrigationScheduleRepository scheduleRepository;
    private final IrrigationLogRepository logRepository;
    private final ZoneRepository zoneRepository;
    private final AgroWeatherService agroWeatherService;
    private final AlertService alertService;

    public IrrigationService(
            IrrigationScheduleRepository scheduleRepository,
            IrrigationLogRepository logRepository,
            ZoneRepository zoneRepository,
            AgroWeatherService agroWeatherService,
            AlertService alertService) {
        this.scheduleRepository = scheduleRepository;
        this.logRepository = logRepository;
        this.zoneRepository = zoneRepository;
        this.agroWeatherService = agroWeatherService;
        this.alertService = alertService;
    }

    public List<IrrigationScheduleResponse> findSchedules() {
        return scheduleRepository.findAll().stream().map(IrrigationScheduleResponse::from).toList();
    }

    public PageResponse<IrrigationScheduleResponse> findSchedules(Pageable pageable, UUID zoneId) {
        Page<IrrigationSchedule> page = zoneId == null
                ? scheduleRepository.findAll(pageable)
                : scheduleRepository.findByZoneId(zoneId, pageable);
        return toPageResponse(page.map(IrrigationScheduleResponse::from));
    }

    @Transactional
    public IrrigationScheduleResponse createSchedule(IrrigationScheduleCreateRequest request) {
        requireZone(request.zoneId());
        IrrigationSchedule schedule = new IrrigationSchedule();
        schedule.setZoneId(request.zoneId());
        schedule.setStartTime(request.startTime());
        schedule.setDurationMinutes(request.durationMinutes());
        schedule.setWaterQuantityLiters(request.waterQuantityLiters());
        schedule.setStatus(request.status() == null ? "scheduled" : request.status());
        // L'authentification est gérée par le logiciel global : sans contexte utilisateur,
        // le planning est attribué à un identifiant système fixe.
        schedule.setCreatedBy(request.createdBy() != null ? request.createdBy() : DEFAULT_SYSTEM_USER_ID);
        return IrrigationScheduleResponse.from(scheduleRepository.save(schedule));
    }

    public IrrigationScheduleResponse getSchedule(UUID scheduleId) {
        return IrrigationScheduleResponse.from(getScheduleEntity(scheduleId));
    }

    @Transactional
    public IrrigationScheduleResponse updateSchedule(UUID scheduleId, IrrigationScheduleUpdateRequest request) {
        IrrigationSchedule schedule = getScheduleEntity(scheduleId);
        if (request.zoneId() != null && !request.zoneId().equals(schedule.getZoneId())) {
            requireZone(request.zoneId());
            schedule.setZoneId(request.zoneId());
        }
        schedule.setStartTime(request.startTime() == null ? schedule.getStartTime() : request.startTime());
        schedule.setDurationMinutes(request.durationMinutes() == null ? schedule.getDurationMinutes() : request.durationMinutes());
        schedule.setWaterQuantityLiters(request.waterQuantityLiters() == null ? schedule.getWaterQuantityLiters() : request.waterQuantityLiters());
        schedule.setStatus(request.status() == null ? schedule.getStatus() : request.status());
        schedule.setCreatedBy(request.createdBy() == null ? schedule.getCreatedBy() : request.createdBy());
        return IrrigationScheduleResponse.from(scheduleRepository.save(schedule));
    }

    @Transactional
    public void deleteSchedule(UUID scheduleId) {
        scheduleRepository.delete(getScheduleEntity(scheduleId));
    }

    public List<IrrigationLogResponse> findLogs() {
        return logRepository.findAll().stream().map(IrrigationLogResponse::from).toList();
    }

    public PageResponse<IrrigationLogResponse> findLogs(Pageable pageable, UUID scheduleId) {
        Page<IrrigationLog> page = scheduleId == null
                ? logRepository.findAll(pageable)
                : logRepository.findByScheduleId(scheduleId, pageable);
        return toPageResponse(page.map(IrrigationLogResponse::from));
    }

    @Transactional
    public IrrigationLogResponse createLog(IrrigationLogCreateRequest request) {
        requireScheduleEntity(request.scheduleId());
        IrrigationLog log = new IrrigationLog();
        log.setScheduleId(request.scheduleId());
        log.setActualStartTime(request.actualStartTime());
        log.setActualEndTime(request.actualEndTime());
        log.setWaterUsedLiters(request.waterUsedLiters());
        log.setStatus(request.status());
        return IrrigationLogResponse.from(logRepository.save(log));
    }

    public IrrigationLogResponse getLog(UUID logId) {
        return IrrigationLogResponse.from(getLogEntity(logId));
    }

    @Transactional
    public IrrigationLogResponse updateLog(UUID logId, IrrigationLogUpdateRequest request) {
        IrrigationLog log = getLogEntity(logId);
        if (request.scheduleId() != null && !request.scheduleId().equals(log.getScheduleId())) {
            requireScheduleEntity(request.scheduleId());
            log.setScheduleId(request.scheduleId());
        }
        log.setActualStartTime(request.actualStartTime() == null ? log.getActualStartTime() : request.actualStartTime());
        log.setActualEndTime(request.actualEndTime() == null ? log.getActualEndTime() : request.actualEndTime());
        log.setWaterUsedLiters(request.waterUsedLiters() == null ? log.getWaterUsedLiters() : request.waterUsedLiters());
        log.setStatus(request.status() == null ? log.getStatus() : request.status());
        return IrrigationLogResponse.from(logRepository.save(log));
    }

    @Transactional
    public void deleteLog(UUID logId) {
        logRepository.delete(getLogEntity(logId));
    }

    @Transactional
    public IrrigationLogResponse start(UUID scheduleId) {
        IrrigationSchedule schedule = getScheduleEntity(scheduleId);
        schedule.setStatus("running");
        scheduleRepository.save(schedule);

        IrrigationLog log = new IrrigationLog();
        log.setScheduleId(scheduleId);
        log.setActualStartTime(Instant.now());
        log.setWaterUsedLiters(0.0);
        log.setStatus("started");
        return IrrigationLogResponse.from(logRepository.save(log));
    }

    @Transactional
    public IrrigationLogResponse stop(UUID scheduleId) {
        IrrigationSchedule schedule = getScheduleEntity(scheduleId);
        schedule.setStatus("completed");
        scheduleRepository.save(schedule);

        Instant stoppedAt = Instant.now();
        IrrigationLog log = logRepository.findFirstByScheduleIdAndStatusOrderByActualStartTimeDesc(scheduleId, "started")
                .orElseGet(() -> {
                    IrrigationLog fallback = new IrrigationLog();
                    fallback.setScheduleId(scheduleId);
                    fallback.setActualStartTime(stoppedAt);
                    return fallback;
                });
        log.setActualEndTime(stoppedAt);
        log.setWaterUsedLiters(schedule.getWaterQuantityLiters() == null ? 0.0 : schedule.getWaterQuantityLiters());
        log.setStatus("completed");
        return IrrigationLogResponse.from(logRepository.save(log));
    }

    /**
     * Suggestions de report fondees sur la pluie prevue : c'est le premier levier d'economie d'eau.
     *
     * <p>Une irrigation planifiee est deconseillee lorsque la pluie prevue le jour du declenchement
     * atteint {@value #RAIN_PROBABILITY_THRESHOLD}% de probabilite et
     * {@value #RAIN_AMOUNT_THRESHOLD_MM} mm. La decision reste humaine : l'API propose, seul
     * {@link #postpone(UUID, String)} applique le report.</p>
     *
     * <p>Si les donnees agro-meteo sont indisponibles, la reponse est renvoyee avec
     * {@code weather_available=false} au lieu d'echouer : le planning doit rester consultable.</p>
     */
    public Map<String, Object> suggestions() {
        List<Map<String, Object>> suggestions = new ArrayList<>();
        boolean weatherAvailable = true;
        try {
            for (IrrigationSchedule schedule : scheduleRepository.findAll()) {
                if (!"scheduled".equalsIgnoreCase(schedule.getStatus()) || schedule.getStartTime() == null) {
                    continue;
                }
                LocalDate day = LocalDate.ofInstant(schedule.getStartTime(), ZoneOffset.UTC);
                Optional<AgroWeatherService.DailyAgro> forecast = agroWeatherService.forDate(day);
                if (forecast.isEmpty()) {
                    continue;
                }
                AgroWeatherService.DailyAgro weather = forecast.get();
                double probability = weather.rainProbability() == null ? 0 : weather.rainProbability();
                double rainMm = weather.rainMm() == null ? 0 : weather.rainMm();
                if (probability < RAIN_PROBABILITY_THRESHOLD || rainMm < RAIN_AMOUNT_THRESHOLD_MM) {
                    continue;
                }
                suggestions.add(suggestion(schedule, probability, rainMm));
            }
        } catch (RuntimeException ex) {
            // Meteo indisponible : on renvoie une liste vide et un indicateur explicite.
            weatherAvailable = false;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("weather_available", weatherAvailable);
        response.put("rain_probability_threshold", RAIN_PROBABILITY_THRESHOLD);
        response.put("rain_amount_threshold_mm", RAIN_AMOUNT_THRESHOLD_MM);
        response.put("suggested_water_saving_liters", suggestions.stream()
                .mapToDouble(item -> ((Number) item.get("potential_saving_liters")).doubleValue()).sum());
        response.put("suggestions", suggestions);
        return response;
    }

    private Map<String, Object> suggestion(IrrigationSchedule schedule, double probability, double rainMm) {
        double planned = schedule.getWaterQuantityLiters() == null ? 0d : schedule.getWaterQuantityLiters();
        String zoneName = zoneRepository.findById(schedule.getZoneId())
                .map(Zone::getName).orElse("Unknown zone");

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("schedule_id", schedule.getId());
        item.put("zone_id", schedule.getZoneId());
        item.put("zone_name", zoneName);
        item.put("start_time", schedule.getStartTime().toString());
        item.put("planned_liters", planned);
        item.put("rain_probability", probability);
        item.put("rain_mm", rainMm);
        item.put("potential_saving_liters", planned);
        item.put("message", "Rain forecast on " + LocalDate.ofInstant(schedule.getStartTime(), ZoneOffset.UTC)
                + ": " + Math.round(rainMm) + " mm (" + Math.round(probability) + "% probability). "
                + "Postponing irrigation of " + zoneName + " would save " + Math.round(planned)
                + " L of supply water.");
        return item;
    }

    /**
     * Postpones a planned irrigation (decision taken after a weather suggestion) and records
     * the matching alert so the achieved water saving is kept in history.
     */
    @Transactional
    public IrrigationScheduleResponse postpone(UUID scheduleId, String reason) {
        IrrigationSchedule schedule = getScheduleEntity(scheduleId);
        if ("completed".equalsIgnoreCase(schedule.getStatus())) {
            throw new IllegalArgumentException("A completed irrigation can no longer be postponed.");
        }
        String previousStatus = schedule.getStatus();
        schedule.setStatus("postponed");
        if (reason != null && !reason.isBlank()) {
            schedule.setPostponeReason(reason);
        }
        IrrigationScheduleResponse response = IrrigationScheduleResponse.from(scheduleRepository.save(schedule));

        double planned = schedule.getWaterQuantityLiters() == null ? 0d : schedule.getWaterQuantityLiters();
        alertService.raise("info", "Irrigation postponed - water saved",
                "Irrigation of " + Math.round(planned) + " L postponed (status: " + previousStatus + " -> postponed)."
                        + (reason == null || reason.isBlank() ? "" : " Reason: " + reason),
                "/irrigation");
        return response;
    }

    private void requireZone(UUID zoneId) {
        if (zoneId == null || zoneRepository.findById(zoneId).isEmpty()) {
            throw new NotFoundException("Zone");
        }
    }

    private IrrigationSchedule requireScheduleEntity(UUID scheduleId) {
        if (scheduleId == null) {
            throw new NotFoundException("Irrigation");
        }
        return getScheduleEntity(scheduleId);
    }

    private IrrigationSchedule getScheduleEntity(UUID scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new NotFoundException("Irrigation"));
    }

    private IrrigationLog getLogEntity(UUID logId) {
        return logRepository.findById(logId)
                .orElseThrow(() -> new NotFoundException("IrrigationLog"));
    }

    private <T> PageResponse<T> toPageResponse(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}