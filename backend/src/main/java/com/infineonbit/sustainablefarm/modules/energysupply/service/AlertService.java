package com.infineonbit.sustainablefarm.modules.energysupply.service;

import com.infineonbit.sustainablefarm.modules.energysupply.dto.EnergyAlertResponse;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.Alert;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.AlertStatus;
import com.infineonbit.sustainablefarm.modules.energysupply.exception.EnergySupplyNotFoundException;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.EnergyAlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("energyAlertService")
@Transactional(readOnly = true)
public class AlertService {

    private final EnergyAlertRepository repository;

    public AlertService(EnergyAlertRepository repository) {
        this.repository = repository;
    }

    public List<EnergyAlertResponse> findAll() {
        return repository.findAll().stream().map(EnergyAlertResponse::from).toList();
    }

    public List<EnergyAlertResponse> findOpen() {
        return repository.findByStatus(AlertStatus.OPEN).stream().map(EnergyAlertResponse::from).toList();
    }

    @Transactional
    public EnergyAlertResponse updateStatus(String alertId, AlertStatus status) {
        Alert alert = repository.findById(alertId)
                .orElseThrow(() -> new EnergySupplyNotFoundException("Energy alert", alertId));
        if (alert.getStatus() == AlertStatus.RESOLVED && status == AlertStatus.OPEN) {
            throw new IllegalArgumentException("Resolved alerts cannot be reopened");
        }
        alert.setStatus(status);
        return EnergyAlertResponse.from(repository.save(alert));
    }
}