package com.infineonbit.sustainablefarm.modules.visitormanagement.controller;

import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.VisitorRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.VisitorResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/visitors")
public class VisitorController {

    private final RegistrationService registrationService;

    public VisitorController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping
    public List<VisitorResponse> list() {
        return registrationService.listVisitors();
    }

    @GetMapping("/{id}")
    public VisitorResponse get(@PathVariable Long id) {
        return registrationService.getVisitor(id);
    }

    @PostMapping
    public ResponseEntity<VisitorResponse> create(@Valid @RequestBody VisitorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registrationService.createVisitor(request));
    }

    @PutMapping("/{id}")
    public VisitorResponse update(@PathVariable Long id, @Valid @RequestBody VisitorRequest request) {
        return registrationService.updateVisitor(id, request);
    }
}
