package com.sustainablefarm.modules.producttransformation.resources.packagingrecord.service;

import com.sustainablefarm.modules.producttransformation.resources.packagingrecord.model.PackagingRecord;

import java.time.LocalDate;
import java.util.List;

/**
 * Service Interface for PackagingRecord Entity
 * Packaging stage data - Core Processing Entity
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
public interface PackagingRecordService {

    /**
     * Create new packaging record
     */
    PackagingRecord createPackagingRecord(PackagingRecord packagingRecord);

    /**
     * Update packaging record
     */
    PackagingRecord updatePackagingRecord(String recordId, PackagingRecord packagingRecord);

    /**
     * Delete packaging record
     */
    void deletePackagingRecord(String recordId);

    /**
     * Get packaging record by ID
     */
    PackagingRecord getPackagingRecordById(String recordId);

    /**
     * Get all packaging records
     */
    List<PackagingRecord> getAllPackagingRecords();

    /**
     * Get packaging records by batch
     */
    List<PackagingRecord> getPackagingRecordsByBatch(String batchId);

    /**
     * Get packaging records by date range
     */
    List<PackagingRecord> getPackagingRecordsByDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * Get packaging records by package type
     */
    List<PackagingRecord> getPackagingRecordsByPackageType(String packageType);

    /**
     * Get packaging records by equipment
     */
    List<PackagingRecord> getPackagingRecordsByEquipment(String equipmentId);

    /**
     * Get packaging records by operator
     */
    List<PackagingRecord> getPackagingRecordsByOperator(String operatorId);

    /**
     * Get packaging records by lot code
     */
    List<PackagingRecord> getPackagingRecordsByLotCode(String lotCode);

    /**
     * Get export-ready packaging records
     * Business Rule: Export ready flag for EU compliance
     */
    List<PackagingRecord> getExportReadyPackagingRecords();

    /**
     * Check if lot code exists
     * Business Rule: Lot codes must be unique for traceability
     */
    boolean lotCodeExists(String lotCode);

    /**
     * Get total packaged quantity by package type
     */
    Double getTotalPackagedQuantityByType(String packageType);

    /**
     * Get export-ready packaging records by date
     */
    List<PackagingRecord> getExportReadyByDate(LocalDate date);

    /**
     * Count export-ready batches
     */
    long countExportReadyBatches();

    /**
     * Mark packaging record as export ready
     * Business Rule: Requires export_ready flag for EU compliance
     */
    PackagingRecord markAsExportReady(String recordId);

    /**
     * Validate lot code uniqueness
     * Business Rule: Lot codes mandatory for traceability compliance
     */
    boolean validateLotCode(String lotCode);
}