package com.sustainablefarm.service.impl;

import com.sustainablefarm.model.PackagingRecord;
import com.sustainablefarm.repository.PackagingRecordRepository;
import com.sustainablefarm.service.PackagingRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service Implementation for PackagingRecord Entity
 * Packaging stage data - Core Processing Entity
 * 
 * Business Rules:
 * - Lot codes mandatory for traceability compliance
 * - Export ready flag for EU compliance
 * - Equipment can be assigned to one batch at a time
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Service
@Transactional
public class PackagingRecordServiceImpl implements PackagingRecordService {

    private final PackagingRecordRepository packagingRecordRepository;

    @Autowired
    public PackagingRecordServiceImpl(PackagingRecordRepository packagingRecordRepository) {
        this.packagingRecordRepository = packagingRecordRepository;
    }

    @Override
    public PackagingRecord createPackagingRecord(PackagingRecord packagingRecord) {
        // Business Rule: Validate lot code uniqueness
        if (packagingRecord.getLotCode() != null && lotCodeExists(packagingRecord.getLotCode())) {
            throw new IllegalArgumentException("Lot code already exists: " + packagingRecord.getLotCode());
        }
        
        // Business Rule: Validate lot code is not empty
        if (packagingRecord.getLotCode() == null || packagingRecord.getLotCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Lot code is mandatory for traceability compliance");
        }
        
        return packagingRecordRepository.save(packagingRecord);
    }

    @Override
    public PackagingRecord updatePackagingRecord(String recordId, PackagingRecord packagingRecord) {
        PackagingRecord existingRecord = getPackagingRecordById(recordId);
        
        // Update fields
        existingRecord.setPackageType(packagingRecord.getPackageType());
        existingRecord.setPackageQuantityKg(packagingRecord.getPackageQuantityKg());
        existingRecord.setLotCode(packagingRecord.getLotCode());
        existingRecord.setExportReady(packagingRecord.getExportReady());
        existingRecord.setPackagingDate(packagingRecord.getPackagingDate());
        existingRecord.setEquipment(packagingRecord.getEquipment());
        existingRecord.setOperator(packagingRecord.getOperator());
        
        return packagingRecordRepository.save(existingRecord);
    }

    @Override
    public void deletePackagingRecord(String recordId) {
        if (!packagingRecordRepository.existsById(recordId)) {
            throw new IllegalArgumentException("Packaging record not found with ID: " + recordId);
        }
        packagingRecordRepository.deleteById(recordId);
    }

    @Override
    @Transactional(readOnly = true)
    public PackagingRecord getPackagingRecordById(String recordId) {
        return packagingRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Packaging record not found with ID: " + recordId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackagingRecord> getAllPackagingRecords() {
        return packagingRecordRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackagingRecord> getPackagingRecordsByBatch(String batchId) {
        return packagingRecordRepository.findByBatchBatchId(batchId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackagingRecord> getPackagingRecordsByDateRange(LocalDate startDate, LocalDate endDate) {
        return packagingRecordRepository.findByPackagingDateBetween(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackagingRecord> getPackagingRecordsByPackageType(String packageType) {
        return packagingRecordRepository.findByPackageType(packageType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackagingRecord> getPackagingRecordsByEquipment(String equipmentId) {
        return packagingRecordRepository.findByEquipmentEquipmentId(equipmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackagingRecord> getPackagingRecordsByOperator(String operatorId) {
        return packagingRecordRepository.findByOperatorOperatorId(operatorId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackagingRecord> getPackagingRecordsByLotCode(String lotCode) {
        return packagingRecordRepository.findByLotCode(lotCode);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackagingRecord> getExportReadyPackagingRecords() {
        // Business Rule: Export ready flag for EU compliance
        return packagingRecordRepository.findByExportReadyTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean lotCodeExists(String lotCode) {
        // Business Rule: Lot codes must be unique for traceability
        return packagingRecordRepository.existsByLotCode(lotCode);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getTotalPackagedQuantityByType(String packageType) {
        return packagingRecordRepository.getTotalPackagedQuantityByType(packageType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackagingRecord> getExportReadyByDate(LocalDate date) {
        return packagingRecordRepository.findExportReadyByDate(date);
    }

    @Override
    @Transactional(readOnly = true)
    public long countExportReadyBatches() {
        return packagingRecordRepository.countExportReadyBatches();
    }

    @Override
    public PackagingRecord markAsExportReady(String recordId) {
        PackagingRecord record = getPackagingRecordById(recordId);
        
        // Business Rule: Requires export_ready flag for EU compliance
        record.markAsExportReady();
        
        return packagingRecordRepository.save(record);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateLotCode(String lotCode) {
        // Business Rule: Lot codes mandatory for traceability compliance
        if (lotCode == null || lotCode.trim().isEmpty()) {
            return false;
        }
        
        // Check uniqueness
        return !lotCodeExists(lotCode);
    }
}