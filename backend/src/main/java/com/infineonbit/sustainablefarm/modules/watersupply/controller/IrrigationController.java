package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.IrrigationLogCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.IrrigationLogResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.IrrigationLogUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.IrrigationPostponeRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.IrrigationScheduleCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.IrrigationScheduleResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.IrrigationScheduleUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.service.IrrigationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.validation.constraints.Min;
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
@RequestMapping("/api")
public class IrrigationController {
    private final IrrigationService irrigationService;

    public IrrigationController(IrrigationService irrigationService) {
        this.irrigationService = irrigationService;
    }

    @GetMapping("/irrigations")
    public Object irrigations(
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Min(1) Integer size,
            @RequestParam(required = false) UUID zoneId) {
        if (page == null && size == null && zoneId == null) return irrigationService.findSchedules();
        return irrigationService.findSchedules(pageRequest(page, size), zoneId);
    }

    @PostMapping("/irrigations")
    @ResponseStatus(HttpStatus.CREATED)
    public IrrigationScheduleResponse createIrrigation(@Valid @RequestBody IrrigationScheduleCreateRequest request) {
        return irrigationService.createSchedule(request);
    }

    @GetMapping("/irrigations/{scheduleId}")
    public IrrigationScheduleResponse irrigation(@PathVariable UUID scheduleId) {
        return irrigationService.getSchedule(scheduleId);
    }

    @PutMapping("/irrigations/{scheduleId}")
    public IrrigationScheduleResponse updateIrrigation(@PathVariable UUID scheduleId, @RequestBody IrrigationScheduleUpdateRequest request) {
        return irrigationService.updateSchedule(scheduleId, request);
    }

    @DeleteMapping("/irrigations/{scheduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIrrigation(@PathVariable UUID scheduleId) {
        irrigationService.deleteSchedule(scheduleId);
    }

    /**
     * Suggestions de report : liste les irrigations planifiees qui peuvent etre evitees
     * parce que la pluie prevue couvrira le besoin (premier levier d'economie d'eau).
     */
    @GetMapping("/irrigation/suggestions")
    public Map<String, Object> suggestions() {
        return irrigationService.suggestions();
    }

    /**
     * Reporte une irrigation planifiee suite a une suggestion (la decision reste humaine).
     */
    @PostMapping("/irrigations/{scheduleId}/postpone")
    public IrrigationScheduleResponse postpone(
            @PathVariable UUID scheduleId,
            @RequestBody(required = false) IrrigationPostponeRequest request) {
        return irrigationService.postpone(scheduleId, request == null ? null : request.reason());
    }

    @GetMapping("/irrigation-logs")
    public Object logs(
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Min(1) Integer size,
            @RequestParam(required = false) UUID scheduleId) {
        if (page == null && size == null && scheduleId == null) return irrigationService.findLogs();
        return irrigationService.findLogs(pageRequest(page, size), scheduleId);
    }

    /**
     * Création d'un log d'irrigation dédié (Tâche 4.3). Permet de saisir un log
     * manuellement sans passer par start/stop implicites.
     */
    @PostMapping("/irrigation-logs")
    @ResponseStatus(HttpStatus.CREATED)
    public IrrigationLogResponse createLog(@Valid @RequestBody IrrigationLogCreateRequest request) {
        return irrigationService.createLog(request);
    }

    @GetMapping("/irrigation-logs/{logId}")
    public IrrigationLogResponse log(@PathVariable UUID logId) {
        return irrigationService.getLog(logId);
    }

    @PutMapping("/irrigation-logs/{logId}")
    public IrrigationLogResponse updateLog(@PathVariable UUID logId, @RequestBody IrrigationLogUpdateRequest request) {
        return irrigationService.updateLog(logId, request);
    }

    @DeleteMapping("/irrigation-logs/{logId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLog(@PathVariable UUID logId) {
        irrigationService.deleteLog(logId);
    }

    @PostMapping("/irrigations/{scheduleId}/start")
    @ResponseStatus(HttpStatus.CREATED)
    public IrrigationLogResponse start(@PathVariable UUID scheduleId) {
        return irrigationService.start(scheduleId);
    }

    @PostMapping("/irrigations/{scheduleId}/stop")
    @ResponseStatus(HttpStatus.CREATED)
    public IrrigationLogResponse stop(@PathVariable UUID scheduleId) {
        return irrigationService.stop(scheduleId);
    }

    private PageRequest pageRequest(Integer page, Integer size) {
        return PageRequest.of(page == null ? 0 : page, size == null ? 20 : size,
                Sort.by(Sort.Direction.ASC, "createdAt"));
    }
}
