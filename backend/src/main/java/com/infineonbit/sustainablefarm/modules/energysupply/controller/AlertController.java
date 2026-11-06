package com.infineonbit.sustainablefarm.modules.energysupply.controller;

import com.infineonbit.sustainablefarm.modules.energysupply.dto.EnergyAlertResponse;
import com.infineonbit.sustainablefarm.modules.energysupply.dto.AlertStatusRequest;
import com.infineonbit.sustainablefarm.modules.energysupply.service.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Feeds the "Alerts & Notifications" tab of the Energy App. */
@RestController("energyAlertController")
@RequestMapping("/api/energy/alerts")
public class AlertController {

    private final AlertService service;

    @Autowired
    public AlertController(AlertService service) {
        this.service = service;
    }

    @GetMapping
    public List<EnergyAlertResponse> getAll() {
        return service.findAll();
    }

    @GetMapping("/open")
    public List<EnergyAlertResponse> getOpen() {
        return service.findOpen();
    }

    @PatchMapping("/{alertId}/status")
    public ResponseEntity<EnergyAlertResponse> updateStatus(
            @PathVariable String alertId,
            @Valid @RequestBody AlertStatusRequest request) {
        return ResponseEntity.ok(service.updateStatus(alertId, request.status()));
    }
}