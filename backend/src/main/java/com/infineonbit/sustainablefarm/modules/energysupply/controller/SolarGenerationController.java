package com.infineonbit.sustainablefarm.modules.energysupply.controller;

import com.infineonbit.sustainablefarm.modules.energysupply.dto.SolarGenerationLogResponse;
import com.infineonbit.sustainablefarm.modules.energysupply.dto.SolarGenerationLogRequest;
import com.infineonbit.sustainablefarm.modules.energysupply.service.SolarGenerationLogService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/** Feeds the "Solar & Generation Monitoring" tab of the Energy App. */
@RestController("energySolarGenerationController")
@RequestMapping("/api/energy/solar")
public class SolarGenerationController {

    private final SolarGenerationLogService service;

    public SolarGenerationController(SolarGenerationLogService service) {
        this.service = service;
    }

    @GetMapping
    public List<SolarGenerationLogResponse> getAll() {
        return service.findAll();
    }

    @GetMapping("/last-7-days")
    public List<SolarGenerationLogResponse> getLast7Days() {
        return service.findLast7Days();
    }

    @GetMapping("/{logId}")
    public SolarGenerationLogResponse getById(@PathVariable String logId) {
        return service.findById(logId);
    }

    @PostMapping
    public ResponseEntity<SolarGenerationLogResponse> create(
            @Valid @RequestBody SolarGenerationLogRequest request) {
        SolarGenerationLogResponse response = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{logId}")
                .buildAndExpand(response.logId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{logId}")
    public SolarGenerationLogResponse update(
            @PathVariable String logId,
            @Valid @RequestBody SolarGenerationLogRequest request) {
        return service.update(logId, request);
    }

    @DeleteMapping("/{logId}")
    public ResponseEntity<Void> delete(@PathVariable String logId) {
        service.delete(logId);
        return ResponseEntity.noContent().build();
    }
}