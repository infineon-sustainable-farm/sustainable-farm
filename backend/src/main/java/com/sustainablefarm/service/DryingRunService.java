package com.sustainablefarm.service;

import com.sustainablefarm.model.DryingRun;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service Interface for DryingRun Entity
 * Drying process data (core transformation) - Core Processing Entity
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
public interface DryingRunService {

    /**
     * Create new drying run
     */
    DryingRun createDryingRun(DryingRun dryingRun);

    /**
     * Update drying run
     */
    DryingRun updateDryingRun(String runId, DryingRun dryingRun);

    /**
     * Delete drying run
     */
    void deleteDryingRun(String runId);

    /**
     * Get drying run by ID
     */
    DryingRun getDryingRunById(String runId);

    /**
     * Get all drying runs
     */
    List<DryingRun> getAllDryingRuns();

    /**
     * Get drying runs by batch
     */
    List<DryingRun> getDryingRunsByBatch(String batchId);

    /**
     * Get drying runs by date range
     */
    List<DryingRun> getDryingRunsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get drying runs by equipment
     */
    List<DryingRun> getDryingRunsByEquipment(String equipmentId);

    /**
     * Get drying runs by operator
     */
    List<DryingRun> getDryingRunsByOperator(String operatorId);

    /**
     * Get drying runs within target moisture range
     * Business Rule: Target moisture 12-18% for EU compliance
     */
    List<DryingRun> getDryingRunsWithinTargetMoistureRange();

    /**
     * Get drying runs below minimum moisture
     */
    List<DryingRun> getDryingRunsBelowMinimumMoisture();

    /**
     * Get drying runs above maximum moisture
     */
    List<DryingRun> getDryingRunsAboveMaximumMoisture();

    /**
     * Get total energy usage by date range
     */
    Double getTotalEnergyUsageByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get average drying duration by batch
     */
    Double getAverageDurationByBatch(String batchId);

    /**
     * Get average moisture reduction by batch
     */
    Double getAverageMoistureReductionByBatch(String batchId);

    /**
     * Get drying runs by equipment type
     */
    List<DryingRun> getDryingRunsByEquipmentType(String equipmentType);

    /**
     * Complete drying run with final moisture validation
     * Business Rule: End moisture content must be 6-18% for EU compliance
     */
    DryingRun completeDryingRun(String runId, BigDecimal endMoisturePct);
}