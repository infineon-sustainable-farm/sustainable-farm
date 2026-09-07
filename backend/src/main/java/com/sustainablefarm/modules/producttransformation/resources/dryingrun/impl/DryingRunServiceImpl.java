package com.sustainablefarm.modules.producttransformation.resources.dryingrun.impl;

import com.sustainablefarm.modules.producttransformation.resources.dryingrun.model.DryingRun;
import com.sustainablefarm.modules.producttransformation.resources.equipment.model.Equipment;
import com.sustainablefarm.modules.producttransformation.resources.operator.model.Operator;
import com.sustainablefarm.modules.producttransformation.resources.dryingrun.repository.DryingRunRepository;
import com.sustainablefarm.modules.producttransformation.resources.equipment.repository.EquipmentRepository;
import com.sustainablefarm.modules.producttransformation.resources.operator.repository.OperatorRepository;
import com.sustainablefarm.modules.producttransformation.resources.dryingrun.service.DryingRunService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final EquipmentRepository equipmentRepository;
    private final OperatorRepository operatorRepository;

    @Autowired
    public DryingRunServiceImpl(DryingRunRepository dryingRunRepository,
                                 EquipmentRepository equipmentRepository,
                                 OperatorRepository operatorRepository) {
        this.dryingRunRepository = dryingRunRepository;
        this.equipmentRepository = equipmentRepository;
        this.operatorRepository = operatorRepository;
    }

    @Override
    public DryingRun createDryingRun(DryingRun dryingRun) {
        // Business Rule: Validate equipment availability before drying
        if (dryingRun.getEquipment() != null) {
            Equipment equipment = dryingRun.getEquipment();
            // Verify equipment exists and is available
            Equipment dbEquipment = equipmentRepository.findById(equipment.getEquipmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Equipment not found with ID: " + equipment.getEquipmentId()));
            
            if (dbEquipment.getMaintenanceStatus() != Equipment.MaintenanceStatus.ACTIVE) {
                throw new IllegalArgumentException(
                    "Equipment is not available for drying. Equipment ID: " + equipment.getEquipmentId() + 
                    " must be in ACTIVE status. Current status: " + dbEquipment.getMaintenanceStatus()
                );
            }
        }
        
        // Business Rule: Validate operator certification
        if (dryingRun.getOperator() != null) {
            Operator operator = dryingRun.getOperator();
            // Verify operator exists and is active
            Operator dbOperator = operatorRepository.findById(operator.getOperatorId())
                    .orElseThrow(() -> new IllegalArgumentException("Operator not found with ID: " + operator.getOperatorId()));
            
            if (dbOperator.getActiveStatus() != Operator.ActiveStatus.ACTIVE) {
                throw new IllegalArgumentException(
                    "Operator is not active for drying. Operator ID: " + operator.getOperatorId() + 
                    " must be in ACTIVE status. Current status: " + dbOperator.getActiveStatus()
                );
            }
            
            // Verify operator has appropriate role for drying operations
            if (dbOperator.getRole() != Operator.Role.DRYER) {
                throw new IllegalArgumentException(
                    "Operator must have DRYER role for drying operations. Operator ID: " + operator.getOperatorId() + 
                    ", Current role: " + dbOperator.getRole()
                );
            }
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
    public DryingRun completeDryingRun(String runId, BigDecimal endMoisturePct) {
        DryingRun dryingRun = getDryingRunById(runId);
        
        // Business Rule: End moisture content must be 6-18% for EU compliance
        // This validation is also done at entity level in @PrePersist/@PreUpdate
        if (endMoisturePct.compareTo(new BigDecimal("6")) < 0 || endMoisturePct.compareTo(new BigDecimal("18")) > 0) {
            throw new IllegalArgumentException(
                "End moisture content must be between 6% and 18% for EU compliance. Current: " + endMoisturePct + "%"
            );
        }
        
        dryingRun.setEndMoisturePct(endMoisturePct);
        dryingRun.setEndTime(LocalDateTime.now());
        
        return dryingRunRepository.save(dryingRun);
    }
}