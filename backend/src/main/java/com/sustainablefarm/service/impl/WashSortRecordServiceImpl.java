package com.sustainablefarm.service.impl;

import com.sustainablefarm.core.exception.BusinessRuleViolationException;
import com.sustainablefarm.core.exception.ResourceNotFoundException;
import com.sustainablefarm.model.WashSortRecord;
import com.sustainablefarm.model.Equipment;
import com.sustainablefarm.model.Operator;
import com.sustainablefarm.repository.WashSortRecordRepository;
import com.sustainablefarm.repository.EquipmentRepository;
import com.sustainablefarm.repository.OperatorRepository;
import com.sustainablefarm.service.WashSortRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service Implementation for WashSortRecord Entity
 * Washing and sorting stage data - Core Processing Entity
 * 
 * Business Rules:
 * - Record created only when batch assigned to washing stage
 * - Equipment can be assigned to one batch at a time
 * - Resource tracking (water usage, yield percentages)
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Service
@Transactional
public class WashSortRecordServiceImpl implements WashSortRecordService {

    private final WashSortRecordRepository washSortRecordRepository;
    private final EquipmentRepository equipmentRepository;
    private final OperatorRepository operatorRepository;

    @Autowired
    public WashSortRecordServiceImpl(WashSortRecordRepository washSortRecordRepository,
                                     EquipmentRepository equipmentRepository,
                                     OperatorRepository operatorRepository) {
        this.washSortRecordRepository = washSortRecordRepository;
        this.equipmentRepository = equipmentRepository;
        this.operatorRepository = operatorRepository;
    }

    @Override
    public WashSortRecord createWashSortRecord(WashSortRecord washSortRecord) {
        // Business Rule: Validate equipment availability
        if (washSortRecord.getEquipment() != null) {
            Equipment equipment = washSortRecord.getEquipment();
            // Verify equipment exists and is available
            Equipment dbEquipment = equipmentRepository.findById(equipment.getEquipmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with ID: " + equipment.getEquipmentId()));
            
            if (dbEquipment.getMaintenanceStatus() != Equipment.MaintenanceStatus.ACTIVE) {
                throw new BusinessRuleViolationException(
                    "Equipment is not available for washing. Equipment ID: " + equipment.getEquipmentId() + 
                    " must be in ACTIVE status. Current status: " + dbEquipment.getMaintenanceStatus()
                );
            }
        }
        
        // Business Rule: Validate operator certification
        if (washSortRecord.getOperator() != null) {
            Operator operator = washSortRecord.getOperator();
            // Verify operator exists and is active
            Operator dbOperator = operatorRepository.findById(operator.getOperatorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Operator not found with ID: " + operator.getOperatorId()));
            
            if (dbOperator.getActiveStatus() != Operator.ActiveStatus.ACTIVE) {
                throw new BusinessRuleViolationException(
                    "Operator is not active for washing. Operator ID: " + operator.getOperatorId() + 
                    " must be in ACTIVE status. Current status: " + dbOperator.getActiveStatus()
                );
            }
            
            // Verify operator has appropriate role for washing operations
            if (dbOperator.getRole() != Operator.Role.WASHER) {
                throw new BusinessRuleViolationException(
                    "Operator must have WASHER role for washing operations. Operator ID: " + operator.getOperatorId() + 
                    ", Current role: " + dbOperator.getRole()
                );
            }
        }
        
        return washSortRecordRepository.save(washSortRecord);
    }

    @Override
    public WashSortRecord updateWashSortRecord(String recordId, WashSortRecord washSortRecord) {
        WashSortRecord existingRecord = getWashSortRecordById(recordId);
        
        // Update fields
        existingRecord.setInputQuantityKg(washSortRecord.getInputQuantityKg());
        existingRecord.setOutputQuantityKg(washSortRecord.getOutputQuantityKg());
        existingRecord.setWasteQuantityKg(washSortRecord.getWasteQuantityKg());
        existingRecord.setWaterUsageLiters(washSortRecord.getWaterUsageLiters());
        existingRecord.setStartTime(washSortRecord.getStartTime());
        existingRecord.setEndTime(washSortRecord.getEndTime());
        existingRecord.setEquipment(washSortRecord.getEquipment());
        existingRecord.setOperator(washSortRecord.getOperator());
        
        return washSortRecordRepository.save(existingRecord);
    }

    @Override
    public void deleteWashSortRecord(String recordId) {
        if (!washSortRecordRepository.existsById(recordId)) {
            throw new ResourceNotFoundException("Wash sort record not found with ID: " + recordId);
        }
        washSortRecordRepository.deleteById(recordId);
    }

    @Override
    @Transactional(readOnly = true)
    public WashSortRecord getWashSortRecordById(String recordId) {
        return washSortRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Wash sort record not found with ID: " + recordId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WashSortRecord> getAllWashSortRecords() {
        return washSortRecordRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WashSortRecord> getWashSortRecordsByBatch(String batchId) {
        return washSortRecordRepository.findByBatchBatchId(batchId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WashSortRecord> getWashSortRecordsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return washSortRecordRepository.findByStartTimeBetween(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WashSortRecord> getWashSortRecordsByEquipment(String equipmentId) {
        return washSortRecordRepository.findByEquipmentEquipmentId(equipmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WashSortRecord> getWashSortRecordsByOperator(String operatorId) {
        return washSortRecordRepository.findByOperatorOperatorId(operatorId);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getTotalWaterUsageByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return washSortRecordRepository.getTotalWaterUsageByDateRange(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAverageYieldPercentageByBatch(String batchId) {
        return washSortRecordRepository.getAverageYieldPercentageByBatch(batchId);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getTotalWasteByBatch(String batchId) {
        return washSortRecordRepository.getTotalWasteByBatch(batchId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WashSortRecord> getLowYieldRecords(Double threshold) {
        return washSortRecordRepository.findLowYieldRecords(threshold);
    }

    @Override
    public WashSortRecord completeWashSortRecord(String recordId, BigDecimal outputQuantityKg, BigDecimal wasteQuantityKg) {
        WashSortRecord record = getWashSortRecordById(recordId);
        
        // Validate quantities
        if (outputQuantityKg.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleViolationException("Output quantity cannot be negative");
        }
        
        if (wasteQuantityKg.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleViolationException("Waste quantity cannot be negative");
        }
        
        // Update final quantities
        record.setOutputQuantityKg(outputQuantityKg);
        record.setWasteQuantityKg(wasteQuantityKg);
        record.setEndTime(LocalDateTime.now());
        
        return washSortRecordRepository.save(record);
    }
}