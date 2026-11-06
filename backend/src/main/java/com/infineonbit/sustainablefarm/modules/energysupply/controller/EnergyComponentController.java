package com.infineonbit.sustainablefarm.modules.energysupply.controller;

import com.infineonbit.sustainablefarm.modules.energysupply.dto.EnergyComponentRequest;
import com.infineonbit.sustainablefarm.modules.energysupply.dto.EnergyComponentResponse;
import com.infineonbit.sustainablefarm.modules.energysupply.service.EnergyComponentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

@RestController("energyComponentController")
@RequestMapping("/api/energy/components")
public class EnergyComponentController {

    private final EnergyComponentService service;

    public EnergyComponentController(EnergyComponentService service) {
        this.service = service;
    }

    @GetMapping
    public List<EnergyComponentResponse> getAll() {
        return service.findAll();
    }

    @GetMapping("/{componentId}")
    public EnergyComponentResponse getById(@PathVariable String componentId) {
        return service.findById(componentId);
    }

    @PostMapping
    public ResponseEntity<EnergyComponentResponse> create(@Valid @RequestBody EnergyComponentRequest request) {
        EnergyComponentResponse response = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{componentId}")
                .buildAndExpand(response.componentId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{componentId}")
    public EnergyComponentResponse update(
            @PathVariable String componentId,
            @Valid @RequestBody EnergyComponentRequest request) {
        if (!componentId.equals(request.componentId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path componentId must match request componentId");
        }
        return service.update(componentId, request);
    }

    @DeleteMapping("/{componentId}")
    public ResponseEntity<Void> delete(@PathVariable String componentId) {
        service.delete(componentId);
        return ResponseEntity.noContent().build();
    }
}