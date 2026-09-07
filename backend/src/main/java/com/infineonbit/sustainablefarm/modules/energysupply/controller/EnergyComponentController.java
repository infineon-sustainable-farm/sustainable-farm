package com.infineonbit.sustainablefarm.modules.energysupply.controller;

import com.infineonbit.sustainablefarm.modules.energysupply.entity.EnergyComponent;
import com.infineonbit.sustainablefarm.modules.energysupply.service.EnergyComponentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/energy/components")
public class EnergyComponentController {

    private final EnergyComponentService service;

    @Autowired
    public EnergyComponentController(EnergyComponentService service) {
        this.service = service;
    }

    @GetMapping
    public List<EnergyComponent> getAll() {
        return service.findAll();
    }

    @GetMapping("/{componentId}")
    public ResponseEntity<EnergyComponent> getById(@PathVariable String componentId) {
        return service.findById(componentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public EnergyComponent create(@RequestBody EnergyComponent component) {
        return service.save(component);
    }

    @PutMapping("/{componentId}")
    public ResponseEntity<EnergyComponent> update(@PathVariable String componentId,
                                                    @RequestBody EnergyComponent updated) {
        return service.findById(componentId)
                .map(existing -> {
                    updated.setComponentId(componentId);
                    return ResponseEntity.ok(service.save(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{componentId}")
    public ResponseEntity<Void> delete(@PathVariable String componentId) {
        service.deleteById(componentId);
        return ResponseEntity.noContent().build();
    }
}
