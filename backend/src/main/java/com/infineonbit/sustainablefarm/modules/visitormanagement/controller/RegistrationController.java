package com.infineonbit.sustainablefarm.modules.visitormanagement.controller;

import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.RegistrationRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.RegistrationResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/registrations")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping
    public List<RegistrationResponse> list(@RequestParam(required = false) Long timeSlotId,
                                           @RequestParam(required = false) LocalDate date,
                                           @RequestParam(required = false) Boolean prospect) {
        if (Boolean.TRUE.equals(prospect)) {
            return registrationService.getProspects();
        }
        if (timeSlotId != null) {
            return registrationService.getRegistrationsBySlot(timeSlotId);
        }
        if (date != null) {
            return registrationService.getRegistrationsByDate(date);
        }
        return List.of();
    }

    @GetMapping("/{id}")
    public RegistrationResponse get(@PathVariable Long id) {
        return registrationService.getRegistration(id);
    }

    @PostMapping
    public ResponseEntity<RegistrationResponse> create(@Valid @RequestBody RegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registrationService.register(request));
    }

    @PatchMapping("/{id}/approve")
    public RegistrationResponse approve(@PathVariable Long id) {
        return registrationService.approve(id);
    }

    @PatchMapping("/{id}/reject")
    public RegistrationResponse reject(@PathVariable Long id) {
        return registrationService.reject(id);
    }

    @PatchMapping("/{id}/check-in")
    public RegistrationResponse checkIn(@PathVariable Long id) {
        return registrationService.checkIn(id);
    }

    @PatchMapping("/{id}/cancel")
    public RegistrationResponse cancel(@PathVariable Long id) {
        return registrationService.cancel(id);
    }
}