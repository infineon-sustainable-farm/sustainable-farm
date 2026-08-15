package com.sustainablefarm.service.impl;

import com.sustainablefarm.model.DryingRun;
import com.sustainablefarm.repository.DryingRunRepository;
import com.sustainablefarm.service.DryingRunService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service Implementation for DryingRun Entity
 * Drying process data (core transformation) - Core Processing Entity
 * 
 * Business Rules:
 * - Energy availability must be confirmed before drying
 * - Target moisture content 12-18% (range: 6-17.44%) for EU compliance
 * - Moisture content validation at entity level (6-18%)
 * - Core transformation process requiring quality control
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Service
@Transactional
public class DryingRunServiceImpl implements DryingRunService {

    private final DryingRunRepository dryingRunRepository;

    @Autowired
    public DryingRunServiceImpl(DryingRunRepository dryingRunRepository) {
        this.dryingRunRepository = dryingRunRepository;
    }

    @Override
    public DryingRun createDryingRun(DryingRun dryingRun) {
        // Business Rule: Validate equipment availability before drying
        if (dryingRun.getEquipment() != null) {
            // Equipment availability check would be done here
            // This is a placeholder for the actual validation
        }
        
        // Business Rule: Validate operator certification
        if (dryingRun.getOperator() != null) {
            // Operator certification check would be done here
            // This is a placeholder for the actual validation
        }
        
        return dryingRunRepository.save(dryingRun);
    }

    @Override
    public DryingRun updateDryingRun(String runId, DryingRun dryingRun) {
        DryingRun existingDryingRun = getDryingRunById(runId);
        
        // Update fields
        existingDryingRun.setDurationHours(dryingRun.getDurationHours());
        existingDryingRun.setTargetTemperatureC(dryingRun.getTargetTemperatureC());
        existingDryingRun.setActualTemperatureC(dryingRun.getActualTemperatureC());
        existingDryingRun.setStartMoisturePct(dryingRun.getStartMoisturePct());
        existingDryingRun.setEndMoisturePct(dryingRun.getEndMoisturePct());
        existingDryingRun.setEnergyUsageKwh(dryingRun.getEnergyUsageKwh());
        existingDryingRun.setStartTime(dryingRun.getStartTime());
        existingDryingRun.setEndTime(dryingRun.getEndTime());
        existingDryingRun.setEquipment(dryingRun.getEquipment());
        existingDryingRun.setOperator(dryingRun.getOperator());
        
        return dryingRunRepository.save(existingDryingRun);
    }

    @Override
    public void deleteDryingRun(String runId) {
        if (!dryingRunRepository.existsById(runId)) {
            throw new IllegalArgumentException("Drying run not found with ID: " + runId);
        }
        dryingRunRepository.deleteById(runId);
    }

    @Override
    @Transactional(readOnly = true)
    public DryingRun getDryingRunById(String runId) {
        return dryingRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Drying run not found with ID: " + runId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DryingRun> getAllDryingRuns() {
        return dryingRunRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DryingRun> getDryingRunsByBatch(String batchId) {
        return dryingRunRepository.findByBatchBatchId(batchId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DryingRun> getDryingRunsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return dryingRunRepository.findByStartTimeBetween(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DryingRun> getDryingRunsByEquipment(String equipmentId) {
        return dryingRunRepository.findByEquipmentEquipmentId(equipmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DryingRun> getDryingRunsByOperator(String operatorId) {
        return dryingRunRepository.findByOperatorOperatorId(operatorId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DryingRun> getDryingRunsWithinTargetMoistureRange() {
        // Business Rule: Target moisture 12-18% for EU compliance
        return dryingRunRepository.findWithinTargetMoistureRange();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DryingRun> getDryingRunsBelowMinimumMoisture() {
        return dryingRunRepository.findBelowMinimumMoisture();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DryingRun> getDryingRunsAboveMaximumMoisture() {
        return dryingRunRepository.findAboveMaximumMoisture();
    }

    @Override
    @Transactional(readOnly = true)
    public Double getTotalEnergyUsageByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return dryingRunRepository.getTotalEnergyUsageByDateRange(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAverageDurationByBatch(String batchId) {
        return dryingRunRepository.getAverageDurationByBatch(batchId);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAverageMoistureReductionByBatch(String batchId) {
        return dryingRunRepository.getAverageMoistureReductionByBatch(batchId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DryingRun> getDryingRunsByEquipmentType(String equipmentType) {
        return dryingRunRepository.findByEquipmentType(equipmentType);
    }

    @Override
    public DryingRun completeDryingRun(String runId, Double endMoisturePct) {
        DryingRun dryingRun = getDryingRunById(runId);
        
        // Business Rule: End moisture content must be 6-18% for EU compliance
        // This validation is also done at entity level in @PrePersist/@PreUpdate
        if (endMoisturePct < 6 || endMoisturePct > 18) {
            throw new IllegalArgumentException(
                "End moisture content must be between 6% and 18% for EU compliance. Current: " + endMoisturePct + "%"
            );
        }
        
        dryingRun.setEndMoisturePct(endMoisturePct);
        dryingRun.setEndTime(LocalDateTime.now());
        
        return dryingRunRepository.save(dryingRun);
    }
}