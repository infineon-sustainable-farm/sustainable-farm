package com.sustainablefarm.service;

import com.sustainablefarm.model.WashSortRecord;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service Interface for WashSortRecord Entity
 * Washing and sorting stage data - Core Processing Entity
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
public interface WashSortRecordService {

    /**
     * Create new wash sort record
     */
    WashSortRecord createWashSortRecord(WashSortRecord washSortRecord);

    /**
     * Update wash sort record
     */
    WashSortRecord updateWashSortRecord(String recordId, WashSortRecord washSortRecord);

    /**
     * Delete wash sort record
     */
    void deleteWashSortRecord(String recordId);

    /**
     * Get wash sort record by ID
     */
    WashSortRecord getWashSortRecordById(String recordId);

    /**
     * Get all wash sort records
     */
    List<WashSortRecord> getAllWashSortRecords();

    /**
     * Get wash sort records by batch
     */
    List<WashSortRecord> getWashSortRecordsByBatch(String batchId);

    /**
     * Get wash sort records by date range
     */
    List<WashSortRecord> getWashSortRecordsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get wash sort records by equipment
     */
    List<WashSortRecord> getWashSortRecordsByEquipment(String equipmentId);

    /**
     * Get wash sort records by operator
     */
    List<WashSortRecord> getWashSortRecordsByOperator(String operatorId);

    /**
     * Get total water usage by date range
     */
    Double getTotalWaterUsageByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get average yield percentage by batch
     */
    Double getAverageYieldPercentageByBatch(String batchId);

    /**
     * Get total waste by batch
     */
    Double getTotalWasteByBatch(String batchId);

    /**
     * Get wash sort records with yield below threshold
     */
    List<WashSortRecord> getLowYieldRecords(Double threshold);

    /**
     * Complete wash sort record with final quantities
     */
    WashSortRecord completeWashSortRecord(String recordId, BigDecimal outputQuantityKg, BigDecimal wasteQuantityKg);
}