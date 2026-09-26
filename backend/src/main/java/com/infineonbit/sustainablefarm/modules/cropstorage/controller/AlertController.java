package com.infineonbit.sustainablefarm.modules.cropstorage.controller;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Alert;
import com.infineonbit.sustainablefarm.modules.cropstorage.service.AlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @PostMapping
    public Alert create(@Valid @RequestBody Alert alert) {
        return alertService.create(alert);
    }

    @GetMapping
    public List<Alert> findAll() {
        return alertService.findAll();
    }

    @GetMapping("/status/{status}")
    public List<Alert> findByStatus(@PathVariable String status) {
        return alertService.findByStatus(status);
    }

    @PatchMapping("/{id}/acknowledge")
    public Alert acknowledge(@PathVariable Long id) {
        return alertService.updateStatus(id, "ACKNOWLEDGED");
    }

    @PatchMapping("/{id}/resolve")
    public Alert resolve(@PathVariable Long id) {
        return alertService.updateStatus(id, "RESOLVED");
    }
}