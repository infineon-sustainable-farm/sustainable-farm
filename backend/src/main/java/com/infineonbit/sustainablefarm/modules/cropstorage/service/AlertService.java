package com.infineonbit.sustainablefarm.modules.cropstorage.service;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Alert;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;

    public Alert create(Alert alert) {
        return alertRepository.save(alert);
    }

    public List<Alert> findAll() {
        return alertRepository.findAll();
    }

    public List<Alert> findByStatus(String status) {
        return alertRepository.findByStatus(status);
    }

    public Alert updateStatus(Long id, String status) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert not found with id " + id));
        alert.setStatus(status);
        return alertRepository.save(alert);
    }
}