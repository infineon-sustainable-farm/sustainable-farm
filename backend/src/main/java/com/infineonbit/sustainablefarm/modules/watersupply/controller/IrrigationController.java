package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationLog;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationSchedule;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IrrigationLogRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IrrigationScheduleRepository;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
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
@RequestMapping("/api")
public class IrrigationController {
    private final IrrigationScheduleRepository scheduleRepository;
    private final IrrigationLogRepository logRepository;

    public IrrigationController(IrrigationScheduleRepository scheduleRepository, IrrigationLogRepository logRepository) {
        this.scheduleRepository = scheduleRepository;
        this.logRepository = logRepository;
    }

    @GetMapping("/irrigations")
    public List<IrrigationSchedule> irrigations() {
        return scheduleRepository.findAll();
    }

    @PostMapping("/irrigations")
    @ResponseStatus(HttpStatus.CREATED)
    public IrrigationSchedule createIrrigation(@Valid @RequestBody IrrigationSchedule schedule) {
        return scheduleRepository.save(schedule);
    }

    @GetMapping("/irrigations/{scheduleId}")
    public IrrigationSchedule irrigation(@PathVariable UUID scheduleId) {
        return scheduleRepository.findById(scheduleId).orElseThrow(() -> new NotFoundException("Irrigation"));
    }

    @PutMapping("/irrigations/{scheduleId}")
    public IrrigationSchedule updateIrrigation(@PathVariable UUID scheduleId, @RequestBody IrrigationSchedule payload) {
        IrrigationSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new NotFoundException("Irrigation"));
        schedule.setZoneId(payload.getZoneId() == null ? schedule.getZoneId() : payload.getZoneId());
        schedule.setStartTime(payload.getStartTime() == null ? schedule.getStartTime() : payload.getStartTime());
        schedule.setDurationMinutes(payload.getDurationMinutes() == null ? schedule.getDurationMinutes() : payload.getDurationMinutes());
        schedule.setWaterQuantityLiters(payload.getWaterQuantityLiters() == null ? schedule.getWaterQuantityLiters() : payload.getWaterQuantityLiters());
        schedule.setStatus(payload.getStatus() == null ? schedule.getStatus() : payload.getStatus());
        schedule.setCreatedBy(payload.getCreatedBy() == null ? schedule.getCreatedBy() : payload.getCreatedBy());
        return scheduleRepository.save(schedule);
    }

    @DeleteMapping("/irrigations/{scheduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIrrigation(@PathVariable UUID scheduleId) {
        scheduleRepository.delete(scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new NotFoundException("Irrigation")));
    }

    @GetMapping("/irrigation-logs")
    public List<IrrigationLog> logs() {
        return logRepository.findAll();
    }

    @PostMapping("/irrigations/{scheduleId}/start")
    @ResponseStatus(HttpStatus.CREATED)
    public IrrigationLog start(@PathVariable UUID scheduleId) {
        IrrigationSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new NotFoundException("Irrigation"));
        schedule.setStatus("running");
        scheduleRepository.save(schedule);

        IrrigationLog log = new IrrigationLog();
        log.setScheduleId(scheduleId);
        log.setActualStartTime(Instant.now());
        log.setWaterUsedLiters(0.0);
        log.setStatus("started");
        return logRepository.save(log);
    }

    @PostMapping("/irrigations/{scheduleId}/stop")
    @ResponseStatus(HttpStatus.CREATED)
    public IrrigationLog stop(@PathVariable UUID scheduleId) {
        IrrigationSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new NotFoundException("Irrigation"));
        schedule.setStatus("completed");
        scheduleRepository.save(schedule);

        IrrigationLog log = new IrrigationLog();
        log.setScheduleId(scheduleId);
        log.setActualStartTime(Instant.now());
        log.setActualEndTime(Instant.now());
        log.setWaterUsedLiters(schedule.getWaterQuantityLiters() == null ? 0.0 : schedule.getWaterQuantityLiters());
        log.setStatus("completed");
        return logRepository.save(log);
    }
}
