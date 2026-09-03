package com.infineonbit.sustainablefarm.modules.visitormanagement.controller;

import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.BriefingDeliverRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.BriefingResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class BriefingController {

    private final RegistrationService registrationService;

    public BriefingController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping("/registrations/{registrationId}/briefing")
    public BriefingResponse get(@PathVariable Long registrationId) {
        return registrationService.getBriefingForRegistration(registrationId);
    }

    @PatchMapping("/registrations/{registrationId}/briefing/deliver")
    public BriefingResponse deliver(@PathVariable Long registrationId,
                                    @Valid @RequestBody BriefingDeliverRequest request) {
        return registrationService.deliverBriefing(registrationId, request);
    }
}