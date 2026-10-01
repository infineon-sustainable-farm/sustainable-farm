package com.infineonbit.sustainablefarm.modules.energysupply.service;

import com.infineonbit.sustainablefarm.modules.energysupply.entity.Alert;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.AlertStatus;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.AlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AlertService {

    private final AlertRepository repository;

    @Autowired
    public AlertService(AlertRepository repository) {
        this.repository = repository;
    }

    public List<Alert> findAll() {
        return repository.findAll();
    }

    public List<Alert> findOpen() {
        return repository.findByStatus(AlertStatus.OPEN);
    }

    public Optional<Alert> findById(String alertId) {
        return repository.findById(alertId);
    }

    public Alert save(Alert alert) {
        return repository.save(alert);
    }

    public void deleteById(String alertId) {
        repository.deleteById(alertId);
    }
}
