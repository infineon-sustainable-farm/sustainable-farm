package com.sustainablefarm.repository;

import com.sustainablefarm.model.QcCheckpoint;
import com.sustainablefarm.model.QcCheckpoint.QcResult;
import com.sustainablefarm.model.QcCheckpoint.QcStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for QcCheckpoint entity
 * Quality control checkpoint data - Core Processing Entity
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Repository
public interface QcCheckpointRepository extends JpaRepository<QcCheckpoint, String> {

    /**
     * Find QC checkpoints by batch
     */
    List<QcCheckpoint> findByBatchBatchId(String batchId);

    /**
     * Find QC checkpoints by stage
     */
    List<QcCheckpoint> findByStage(QcStage stage);

    /**
     * Find QC checkpoints by result
     */
    List<QcCheckpoint> findByResult(QcResult result);

    /**
     * Find QC checkpoints by inspector
     */
    List<QcCheckpoint> findByInspectorOperatorId(String inspectorId);

    /**
     * Find QC checkpoints by date range
     */
    List<QcCheckpoint> findByCheckpointTimeBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find QC checkpoints by batch and stage
     */
    @Query("SELECT q FROM QcCheckpoint q WHERE q.batch.batchId = :batchId AND q.stage = :stage")
    List<QcCheckpoint> findByBatchAndStage(@Param("batchId") String batchId, @Param("stage") QcStage stage);

    /**
     * Find QC checkpoints by batch and result
     */
    @Query("SELECT q FROM QcCheckpoint q WHERE q.batch.batchId = :batchId AND q.result = :result")
    List<QcCheckpoint> findByBatchAndResult(@Param("batchId") String batchId, @Param("result") QcResult result);

    /**
     * Find mandatory QC checkpoints
     * Business Rule: Mandatory at washing (Phase 2) and cooling (Phase 5) stages
     */
    @Query("SELECT q FROM QcCheckpoint q WHERE q.stage IN ('WASHING', 'COOLING')")
    List<QcCheckpoint> findMandatoryCheckpoints();

    /**
     * Find failed QC checkpoints
     */
    @Query("SELECT q FROM QcCheckpoint q WHERE q.result = 'FAIL'")
    List<QcCheckpoint> findFailedCheckpoints();

    /**
     * Count failed checkpoints by batch
     */
    @Query("SELECT COUNT(q) FROM QcCheckpoint q WHERE q.batch.batchId = :batchId AND q.result = 'FAIL'")
    long countFailedByBatch(@Param("batchId") String batchId);

    /**
     * Count checkpoints by batch and stage
     */
    @Query("SELECT COUNT(q) FROM QcCheckpoint q WHERE q.batch.batchId = :batchId AND q.stage = :stage")
    long countByBatchAndStage(@Param("batchId") String batchId, @Param("stage") QcStage stage);

    /**
     * Find QC checkpoints by batch and date range
     */
    @Query("SELECT q FROM QcCheckpoint q WHERE q.batch.batchId = :batchId AND q.checkpointTime BETWEEN :startDate AND :endDate")
    List<QcCheckpoint> findByBatchAndDateRange(@Param("batchId") String batchId,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    /**
     * Get pass rate by stage
     */
    @Query("SELECT COUNT(q) FROM QcCheckpoint q WHERE q.stage = :stage AND q.result = 'PASS'")
    long countPassedByStage(@Param("stage") QcStage stage);

    /**
     * Get total checkpoints by stage
     */
    @Query("SELECT COUNT(q) FROM QcCheckpoint q WHERE q.stage = :stage")
    long countTotalByStage(@Param("stage") QcStage stage);
}