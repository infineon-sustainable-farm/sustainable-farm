package com.infineonbit.sustainablefarm.modules.energysupply.controller;

import com.infineonbit.sustainablefarm.modules.energysupply.dto.ConsumptionLogRequest;
import com.infineonbit.sustainablefarm.modules.energysupply.dto.ConsumptionLogResponse;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.LoadCategory;
import com.infineonbit.sustainablefarm.modules.energysupply.service.ConsumptionLogService;
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

/** Feeds the "Energy Consumption Tracking" tab of the Energy App. */
@RestController("energyConsumptionController")
@RequestMapping("/api/energy/consumption")
public class ConsumptionController {

    private final ConsumptionLogService service;

    public ConsumptionController(ConsumptionLogService service) {
        this.service = service;
    }

    @GetMapping
    public List<ConsumptionLogResponse> getAll() {
        return service.findAll();
    }

    @GetMapping("/today")
    public List<ConsumptionLogResponse> getToday() {
        return service.findToday();
    }

    @GetMapping("/breakdown/{category}")
    public List<ConsumptionLogResponse> getByCategory(@PathVariable LoadCategory category) {
        return service.findByCategory(category);
    }

    @GetMapping("/{logId}")
    public ConsumptionLogResponse getById(@PathVariable String logId) {
        return service.findById(logId);
    }

    @PostMapping
    public ResponseEntity<ConsumptionLogResponse> create(@Valid @RequestBody ConsumptionLogRequest request) {
        ConsumptionLogResponse response = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{logId}")
                .buildAndExpand(response.logId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{logId}")
    public ConsumptionLogResponse update(
            @PathVariable String logId,
            @Valid @RequestBody ConsumptionLogRequest request) {
        return service.update(logId, request);
    }

    @DeleteMapping("/{logId}")
    public ResponseEntity<Void> delete(@PathVariable String logId) {
        service.delete(logId);
        return ResponseEntity.noContent().build();
    }
}