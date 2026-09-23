package com.sustainablefarm.modules.producttransformation.resources.qccheckpoint.service;

import com.sustainablefarm.modules.producttransformation.resources.qccheckpoint.model.QcCheckpoint;
import com.sustainablefarm.modules.producttransformation.resources.qccheckpoint.model.QcCheckpoint.QcResult;
import com.sustainablefarm.modules.producttransformation.resources.qccheckpoint.model.QcCheckpoint.QcStage;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service Interface for QcCheckpoint Entity
 * Quality control checkpoint data - Core Processing Entity
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
public interface QcCheckpointService {

    /**
     * Create new QC checkpoint
     */
    QcCheckpoint createQcCheckpoint(QcCheckpoint qcCheckpoint);

    /**
     * Update QC checkpoint
     */
    QcCheckpoint updateQcCheckpoint(String checkpointId, QcCheckpoint qcCheckpoint);

    /**
     * Delete QC checkpoint
     */
    void deleteQcCheckpoint(String checkpointId);

    /**
     * Get QC checkpoint by ID
     */
    QcCheckpoint getQcCheckpointById(String checkpointId);

    /**
     * Get all QC checkpoints
     */
    List<QcCheckpoint> getAllQcCheckpoints();

    /**
     * Get QC checkpoints by batch
     */
    List<QcCheckpoint> getQcCheckpointsByBatch(String batchId);

    /**
     * Get QC checkpoints by stage
     */
    List<QcCheckpoint> getQcCheckpointsByStage(QcStage stage);

    /**
     * Get QC checkpoints by result
     */
    List<QcCheckpoint> getQcCheckpointsByResult(QcResult result);

    /**
     * Get QC checkpoints by inspector
     */
    List<QcCheckpoint> getQcCheckpointsByInspector(String inspectorId);

    /**
     * Get QC checkpoints by date range
     */
    List<QcCheckpoint> getQcCheckpointsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get QC checkpoints by batch and stage
     */
    List<QcCheckpoint> getQcCheckpointsByBatchAndStage(String batchId, QcStage stage);

    /**
     * Get QC checkpoints by batch and result
     */
    List<QcCheckpoint> getQcCheckpointsByBatchAndResult(String batchId, QcResult result);

    /**
     * Get mandatory QC checkpoints
     * Business Rule: Mandatory at washing (Phase 2) and cooling (Phase 5) stages
     */
    List<QcCheckpoint> getMandatoryCheckpoints();

    /**
     * Get failed QC checkpoints
     */
    List<QcCheckpoint> getFailedCheckpoints();

    /**
     * Count failed checkpoints by batch
     */
    long countFailedByBatch(String batchId);

    /**
     * Count checkpoints by batch and stage
     */
    long countByBatchAndStage(String batchId, QcStage stage);

    /**
     * Get pass rate by stage
     */
    double getPassRateByStage(QcStage stage);

    /**
     * Check if batch has mandatory checkpoints completed
     * Business Rule: Mandatory at washing (Phase 2) and cooling (Phase 5) stages
     */
    boolean hasMandatoryCheckpointsCompleted(String batchId);

    /**
     * Create mandatory checkpoint for batch
     * Business Rule: Ensures mandatory checkpoints are created
     */
    QcCheckpoint createMandatoryCheckpoint(String batchId, QcStage stage, String inspectorId);
}