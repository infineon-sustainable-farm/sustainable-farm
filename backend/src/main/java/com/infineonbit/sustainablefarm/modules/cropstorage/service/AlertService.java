package com.infineonbit.sustainablefarm.modules.cropstorage.service;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Alert;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Manages the alert lifecycle for Crop Storage: manual creation, listing and
 * status transitions ACTIVE to ACKNOWLEDGED to RESOLVED. Automatic threshold
 * alerts are raised by SensorReadingService, not here.
 */
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;

    /**
     * Saves a manually created alert, for issues an operator observes and the
     * sensors did not detect (smell, visual damage, door left open...).
     *
     * @param alert alert built by the caller: type, severity, message, status
     *              and optional links to sensor, zone or batch
     * @return the saved alert with its generated id
     */
    public Alert create(Alert alert) {
        return alertRepository.save(alert);
    }

    /**
     * Returns every alert of the module, whatever its status.
     *
     * @return all alerts, including acknowledged and resolved ones
     */
    public List<Alert> findAll() {
        return alertRepository.findAll();
    }

    /**
     * Returns the alerts currently in one given status; the dashboard Active
     * Alerts panel relies on the ACTIVE view.
     *
     * @param status one of ACTIVE, ACKNOWLEDGED, RESOLVED
     * @return alerts matching that status
     */
    public List<Alert> findByStatus(String status) {
        return alertRepository.findByStatus(status);
    }

    /**
     * Moves an alert to a new status; backs the acknowledge and resolve
     * endpoints used by the operator once the issue is handled.
     *
     * @param id     id of the alert to move
     * @param status target status, typically ACKNOWLEDGED or RESOLVED
     * @return the alert with its new status
     * @throws RuntimeException when no alert exists with this id
     */
    public Alert updateStatus(Long id, String status) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert not found with id " + id));
        alert.setStatus(status);
        return alertRepository.save(alert);
    }
}