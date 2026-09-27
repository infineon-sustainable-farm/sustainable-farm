package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.common.ResourceNotFoundException;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Alert;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.AlertRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlertService {

    private final AlertRepository repository;

    public AlertService(AlertRepository repository) {
        this.repository = repository;
    }

    public List<Alert> getAll() {
        return repository.findAll();
    }

    public Alert getById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found with id: " + id));
    }

    public Alert create(Alert item) {
        return repository.save(item);
    }

    /**
     * Copies every editable field from updatedData onto the existing
     * row, then saves it. The id and created_at are left untouched.
     */
    public Alert update(Integer id, Alert updatedData) {
        Alert existing = getById(id);

        existing.setAlertCode(updatedData.getAlertCode());
        existing.setAlertType(updatedData.getAlertType());
        existing.setSeverity(updatedData.getSeverity());
        existing.setMessage(updatedData.getMessage());
        existing.setRegion(updatedData.getRegion());
        existing.setOrigin(updatedData.getOrigin());
        existing.setRelatedTable(updatedData.getRelatedTable());
        existing.setRelatedId(updatedData.getRelatedId());
        existing.setStatus(updatedData.getStatus());
        existing.setResolvedAt(updatedData.getResolvedAt());

        return repository.save(existing);
    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }
}