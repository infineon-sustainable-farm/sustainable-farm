package com.infineonbit.sustainablefarm.modules.producttransformation.qccheckpoint.repository;

import com.infineonbit.sustainablefarm.modules.producttransformation.qccheckpoint.model.QcCheckpoint;
import com.infineonbit.sustainablefarm.modules.producttransformation.qccheckpoint.model.QcResult;
import com.infineonbit.sustainablefarm.modules.producttransformation.qccheckpoint.model.QcStage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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
public interface QcCheckpointRepository extends JpaRepository<QcCheckpoint, Long> {

    /** The QcCheckpoint of a business code, shown as {@code checkpointId}. */
    @EntityGraph(attributePaths = {"batch", "inspector"})
    Optional<QcCheckpoint> findByCheckpointId(String checkpointId);

    /** Whether a QcCheckpoint has this business code. */
    boolean existsByCheckpointId(String checkpointId);

    /** Deletes the row of a business code, with what it cascades to. */
    void deleteByCheckpointId(String checkpointId);


    /**
     * Find QC checkpoints by batch
     */
    @EntityGraph(attributePaths = {"batch", "inspector"})
    List<QcCheckpoint> findByBatchBatchId(String batchId);

    /**
     * Find QC checkpoints by stage
     */
    @EntityGraph(attributePaths = {"batch", "inspector"})
    List<QcCheckpoint> findByStage(QcStage stage);

    /**
     * Find QC checkpoints by result
     */
    @EntityGraph(attributePaths = {"batch", "inspector"})
    List<QcCheckpoint> findByResult(QcResult result);

    /**
     * Find QC checkpoints by inspector
     */
    @EntityGraph(attributePaths = {"batch", "inspector"})
    List<QcCheckpoint> findByInspectorOperatorId(String inspectorId);

    /**
     * Aggregate QC checkpoints by checkpoint_time range (SQL-side aggregation).
     * Used for pass rate KPIs without loading checkpoint rows.
     */
    @Query("SELECT COUNT(q) FROM QcCheckpoint q WHERE q.checkpointTime BETWEEN :start AND :end")
    long countByCheckpointTimeBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count QC checkpoints by checkpoint_time range and result
     */
    @Query("SELECT COUNT(q) FROM QcCheckpoint q WHERE q.checkpointTime BETWEEN :start AND :end AND q.result = :result")
    long countByCheckpointTimeBetweenAndResult(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                                               @Param("result") QcResult result);

    /**
     * Find QC checkpoints by date range
     */
    @EntityGraph(attributePaths = {"batch", "inspector"})
    List<QcCheckpoint> findByCheckpointTimeBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find QC checkpoints by checkpoint date range (LocalDate)
     */
    @Query("SELECT q FROM QcCheckpoint q WHERE DATE(q.checkpointTime) BETWEEN :startDate AND :endDate")
    @EntityGraph(attributePaths = {"batch", "inspector"})
    List<QcCheckpoint> findByCheckpointDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find QC checkpoints by batch and stage
     */
    @Query("SELECT q FROM QcCheckpoint q WHERE q.batch.batchId = :batchId AND q.stage = :stage")
    @EntityGraph(attributePaths = {"batch", "inspector"})
    List<QcCheckpoint> findByBatchAndStage(@Param("batchId") String batchId, @Param("stage") QcStage stage);

    /**
     * Find QC checkpoints by batch and result
     */
    @Query("SELECT q FROM QcCheckpoint q WHERE q.batch.batchId = :batchId AND q.result = :result")
    @EntityGraph(attributePaths = {"batch", "inspector"})
    List<QcCheckpoint> findByBatchAndResult(@Param("batchId") String batchId, @Param("result") QcResult result);

    /**
     * Find mandatory QC checkpoints
     * Business Rule: Mandatory at washing (Phase 2) and cooling (Phase 5) stages
     */
    @Query("SELECT q FROM QcCheckpoint q WHERE q.stage IN ('WASHING', 'COOLING')")
    @EntityGraph(attributePaths = {"batch", "inspector"})
    List<QcCheckpoint> findMandatoryCheckpoints();

    /**
     * Find failed QC checkpoints
     */
    @Query("SELECT q FROM QcCheckpoint q WHERE q.result = 'FAIL'")
    @EntityGraph(attributePaths = {"batch", "inspector"})
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
    @EntityGraph(attributePaths = {"batch", "inspector"})
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

    /** Every row, with what the responses show fetched in the same query (no N+1). */
    @Override
    @EntityGraph(attributePaths = {"batch", "inspector"})
    List<QcCheckpoint> findAll();
}