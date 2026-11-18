package com.infineonbit.sustainablefarm.modules.producttransformation.dryingrun.impl;

import com.infineonbit.sustainablefarm.modules.producttransformation.operator.model.Role;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.model.ActiveStatus;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.MaintenanceStatus;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.EquipmentType;
import com.infineonbit.sustainablefarm.modules.producttransformation.dryingrun.model.DryingRun;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.Equipment;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.model.Operator;
import com.infineonbit.sustainablefarm.modules.producttransformation.dryingrun.repository.DryingRunRepository;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.service.EquipmentService;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.service.OperatorService;
import com.infineonbit.sustainablefarm.modules.producttransformation.dryingrun.service.DryingRunService;
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
    private final EquipmentService equipmentService;
    private final OperatorService operatorService;

    @Autowired
    public DryingRunServiceImpl(DryingRunRepository dryingRunRepository,
                                 EquipmentService equipmentService,
                                 OperatorService operatorService) {
        this.dryingRunRepository = dryingRunRepository;
        this.equipmentService = equipmentService;
        this.operatorService = operatorService;
    }

    @Override
    public DryingRun createDryingRun(DryingRun dryingRun) {
        // Business Rule: Validate equipment availability before drying
        if (dryingRun.getEquipmentId() != null) {
            // Verify equipment exists and is available
            Equipment dbEquipment = equipmentService.getEquipmentByRegisterId(dryingRun.getEquipmentId());
            
            if (dbEquipment.getMaintenanceStatus() != MaintenanceStatus.ACTIVE) {
                throw new IllegalArgumentException(
                    "Equipment is not available for drying. Equipment ID: " + dbEquipment.getEquipmentId() + 
                    " must be in ACTIVE status. Current status: " + dbEquipment.getMaintenanceStatus()
                );
            }
        }
        
        // Business Rule: Validate operator certification
        if (dryingRun.getOperator() != null) {
            Operator operator = dryingRun.getOperator();
            // Verify operator exists and is active
            Operator dbOperator = operatorService.findOperatorById(operator.getOperatorId())
                    .orElseThrow(() -> new IllegalArgumentException("Operator not found with ID: " + operator.getOperatorId()));
            
            if (dbOperator.getActiveStatus() != ActiveStatus.ACTIVE) {
                throw new IllegalArgumentException(
                    "Operator is not active for drying. Operator ID: " + operator.getOperatorId() + 
                    " must be in ACTIVE status. Current status: " + dbOperator.getActiveStatus()
                );
            }
            
            // Verify operator has appropriate role for drying operations
            if (dbOperator.getRole() != Role.DRYER) {
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
        existingDryingRun.setEquipmentId(dryingRun.getEquipmentId());
        existingDryingRun.setOperator(dryingRun.getOperator());
        
        return dryingRunRepository.save(existingDryingRun);
    }

    @Override
    public void deleteDryingRun(String runId) {
        if (!dryingRunRepository.existsByRunId(runId)) {
            throw new IllegalArgumentException("Drying run not found with ID: " + runId);
        }
        dryingRunRepository.deleteByRunId(runId);
    }

    @Override
    @Transactional(readOnly = true)
    public DryingRun getDryingRunById(String runId) {
        return dryingRunRepository.findByRunId(runId)
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
        return equipmentService.findEquipmentById(equipmentId)
                .map(equipment -> dryingRunRepository.findByEquipmentId(equipment.getId()))
                .orElse(List.of());
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
        EquipmentType type;
        try {
            type = EquipmentType.valueOf(equipmentType);
        } catch (IllegalArgumentException | NullPointerException unknownType) {
            return List.of();
        }
        return dryingRunRepository.findByEquipmentType(type);
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