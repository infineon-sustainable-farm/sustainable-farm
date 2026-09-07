package com.infineonbit.sustainablefarm.modules.energysupply.controller;

import com.infineonbit.sustainablefarm.modules.energysupply.entity.Alert;
import com.infineonbit.sustainablefarm.modules.energysupply.service.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Feeds the "Alerts & Notifications" tab of the Energy App. */
@RestController
@RequestMapping("/api/energy/alerts")
public class AlertController {

    private final AlertService service;

    @Autowired
    public AlertController(AlertService service) {
        this.service = service;
    }

    @GetMapping
    public List<Alert> getAll() {
        return service.findAll();
    }

    @GetMapping("/open")
    public List<Alert> getOpen() {
        return service.findOpen();
    }

    @PostMapping
    public Alert create(@RequestBody Alert alert) {
        return service.save(alert);
    }
}
