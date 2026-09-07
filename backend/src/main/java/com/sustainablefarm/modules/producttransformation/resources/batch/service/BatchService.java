package com.sustainablefarm.modules.producttransformation.resources.batch.service;

import com.sustainablefarm.modules.producttransformation.resources.batch.model.Batch;
import com.sustainablefarm.modules.producttransformation.resources.batch.model.Batch.BatchStatus;
import com.sustainablefarm.modules.producttransformation.resources.harvestevent.model.HarvestEvent.MangoVariety;

import java.time.LocalDate;
import java.util.List;

/**
 * Service Interface for Batch Entity
 * Central traceability entity for mango processing - Core Processing Entity
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
public interface BatchService {

    /**
     * Create new batch
     */
    Batch createBatch(Batch batch);

    /**
     * Update batch
     */
    Batch updateBatch(String batchId, Batch batch);

    /**
     * Delete batch
     */
    void deleteBatch(String batchId);

    /**
     * Get batch by ID
     */
    Batch getBatchById(String batchId);

    /**
     * Get all batches
     */
    List<Batch> getAllBatches();

    /**
     * Get batches by current status
     */
    List<Batch> getBatchesByStatus(BatchStatus status);

    /**
     * Get batches by harvest date range
     */
    List<Batch> getBatchesByHarvestDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * Get batches by mango variety
     */
    List<Batch> getBatchesByVariety(MangoVariety variety);

    /**
     * Get batches by farm
     */
    List<Batch> getBatchesByFarm(String farmId);

    /**
     * Get batches by farm and status
     */
    List<Batch> getBatchesByFarmAndStatus(String farmId, BatchStatus status);

    /**
     * Get batches by variety and status
     */
    List<Batch> getBatchesByVarietyAndStatus(MangoVariety variety, BatchStatus status);

    /**
     * Count batches by status
     */
    long countBatchesByStatus(BatchStatus status);

    /**
     * Get total harvest quantity by variety
     */
    Double getTotalHarvestQuantityByVariety(MangoVariety variety);

    /**
     * Get batches ready for shipping
     */
    List<Batch> getBatchesReadyForShipping();

    /**
     * Get batches by block
     */
    List<Batch> getBatchesByBlock(String blockId);

    /**
     * Get batches by farm and block
     */
    List<Batch> getBatchesByFarmAndBlock(String farmId, String blockId);

    /**
     * Advance batch status to next stage
     * Business Rule: Status transitions must follow the processing workflow
     */
    Batch advanceBatchStatus(String batchId);

    /**
     * Set batch status to specific status
     */
    Batch setBatchStatus(String batchId, BatchStatus status);

    /**
     * Check if batch exists with specific status
     */
    boolean existsByBatchIdAndStatus(String batchId, BatchStatus status);
}