package com.infineonbit.sustainablefarm.modules.visitormanagement.controller;

import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.AvailabilityResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.TimeSlotRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.TimeSlotResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.service.SchedulingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/time-slots")
public class TimeSlotController {

    private final SchedulingService schedulingService;

    public TimeSlotController(SchedulingService schedulingService) {
        this.schedulingService = schedulingService;
    }

    @GetMapping
    public List<TimeSlotResponse> list(@RequestParam(required = false) LocalDate date) {
        if (date != null) {
            return schedulingService.findByDate(date);
        }
        return schedulingService.findAll();
    }

    @GetMapping("/{id:\\d+}")
    public TimeSlotResponse get(@PathVariable Long id) {
        return schedulingService.findById(id);
    }

    @PostMapping
    public ResponseEntity<TimeSlotResponse> create(@Valid @RequestBody TimeSlotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schedulingService.create(request));
    }

    @PutMapping("/{id}")
    public TimeSlotResponse update(@PathVariable Long id,
                                   @Valid @RequestBody TimeSlotRequest request) {
        return schedulingService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        schedulingService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/assign-guide")
    public TimeSlotResponse assignGuide(@PathVariable Long id,
                                        @RequestParam Long guideId) {
        return schedulingService.assignGuide(id, guideId);
    }

    @GetMapping("/availability")
    public List<AvailabilityResponse> availability(@RequestParam LocalDate date) {
        return schedulingService.getAvailability(date);
    }
}
