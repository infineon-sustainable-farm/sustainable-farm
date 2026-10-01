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

/**
 * REST endpoints of the alert board: full listing, listing by status, manual
 * creation, and the two operator actions acknowledge and resolve.
 */
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    /**
     * Creates a manual alert; threshold-crossing alerts are created
     * automatically by the sensor reading flow instead.
     *
     * @param alert alert fields as described in AlertService
     * @return the saved alert
     */
    @PostMapping
    public Alert create(@Valid @RequestBody Alert alert) {
        return alertService.create(alert);
    }

    /**
     * Returns every alert, all statuses mixed, for the history view.
     *
     * @return all alerts
     */
    @GetMapping
    public List<Alert> findAll() {
        return alertService.findAll();
    }

    /**
     * Returns the alerts in one status; GET /api/alerts/status/ACTIVE feeds
     * the Active Alerts panel of the dashboard.
     *
     * @param status ACTIVE, ACKNOWLEDGED or RESOLVED
     * @return alerts in that status
     */
    @GetMapping("/status/{status}")
    public List<Alert> findByStatus(@PathVariable String status) {
        return alertService.findByStatus(status);
    }

    /**
     * Marks an alert as seen by the operator: status becomes ACKNOWLEDGED.
     *
     * @param id alert to acknowledge
     * @return the acknowledged alert
     */
    @PatchMapping("/{id}/acknowledge")
    public Alert acknowledge(@PathVariable Long id) {
        return alertService.updateStatus(id, "ACKNOWLEDGED");
    }

    /**
     * Closes an alert once corrected: status becomes RESOLVED.
     *
     * @param id alert to resolve
     * @return the resolved alert
     */
    @PatchMapping("/{id}/resolve")
    public Alert resolve(@PathVariable Long id) {
        return alertService.updateStatus(id, "RESOLVED");
    }
}