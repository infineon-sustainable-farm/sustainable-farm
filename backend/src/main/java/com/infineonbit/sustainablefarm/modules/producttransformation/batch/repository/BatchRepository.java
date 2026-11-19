package com.infineonbit.sustainablefarm.modules.producttransformation.batch.repository;

import com.infineonbit.sustainablefarm.modules.producttransformation.batch.model.Batch;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.model.BatchStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Batch entity
 * Central traceability entity for mango processing - Core Processing Entity
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {

    /** The Batch of a business code, shown as {@code batchId}. */
    @EntityGraph(attributePaths = {"rawIntake"})
    Optional<Batch> findByBatchId(String batchId);

    /** Whether a Batch has this business code. */
    boolean existsByBatchId(String batchId);

    /** Deletes the row of a business code, with what it cascades to. */
    void deleteByBatchId(String batchId);


    /**
     * Find batches by current status
     */
    @EntityGraph(attributePaths = {"rawIntake"})
    List<Batch> findByCurrentStatus(BatchStatus currentStatus);

    /**
     * Find batches by harvest date range
     */
    @EntityGraph(attributePaths = {"rawIntake"})
    List<Batch> findByHarvestDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find batches by mango variety
     */
    @EntityGraph(attributePaths = {"rawIntake"})
    List<Batch> findByVarietyKey(String varietyKey);

    /**
     * Find batches of several blocks (the blocks of a farm)
     */
    @EntityGraph(attributePaths = {"rawIntake"})
    List<Batch> findByFarmBlockIdIn(java.util.Collection<Long> farmBlockIds);

    /**
     * Find batches of several blocks (the blocks of a farm) and a status
     */
    @EntityGraph(attributePaths = {"rawIntake"})
    List<Batch> findByFarmBlockIdInAndCurrentStatus(java.util.Collection<Long> farmBlockIds, BatchStatus currentStatus);

    /**
     * Find batches by variety and status
     */
    @Query("SELECT b FROM Batch b WHERE b.varietyKey = :variety AND b.currentStatus = :status")
    @EntityGraph(attributePaths = {"rawIntake"})
    List<Batch> findByVarietyAndStatus(@Param("variety") String variety, @Param("status") BatchStatus status);

    /**
     * Count batches by status
     */
    @Query("SELECT COUNT(b) FROM Batch b WHERE b.currentStatus = :status")
    long countByStatus(@Param("status") BatchStatus status);

    /**
     * Get total harvest quantity by variety
     */
    @Query("SELECT SUM(b.harvestQuantityKg) FROM Batch b WHERE b.varietyKey = :variety")
    Double getTotalHarvestQuantityByVariety(@Param("variety") String variety);

    /**
     * Find batches ready for shipping
     */
    @Query("SELECT b FROM Batch b WHERE b.currentStatus = 'COMPLETED'")
    @EntityGraph(attributePaths = {"rawIntake"})
    List<Batch> findReadyForShipping();

    /**
     * Find batches by block
     */
    @EntityGraph(attributePaths = {"rawIntake"})
    List<Batch> findByFarmBlockId(Long farmBlockId);

    /**
     * Check if batch exists by status
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Batch b WHERE b.batchId = :id AND b.currentStatus = :status")
    boolean existsByBatchIdAndCurrentStatus(@Param("id") String batchId, @Param("status") BatchStatus status);

    /**
     * Count batches by several statuses (aggregate query for dashboard KPIs).
     * Used to avoid loading every batch just to count active ones.
     */
    @Query("SELECT COUNT(b) FROM Batch b WHERE b.currentStatus IN :statuses")
    long countByCurrentStatusIn(@Param("statuses") Collection<BatchStatus> statuses);

    /**
     * First 10 active batches (dashboard summary list).
     * Bounded in SQL: the previous code filtered {@code findAll()} in memory, so the
     * database still returned the whole table just to keep ten rows (quality-gate W1).
     */
    @EntityGraph(attributePaths = {"rawIntake"})
    List<Batch> findTop10ByCurrentStatusIn(@Param("statuses") Collection<BatchStatus> statuses);

    /** Every row, with what the responses show fetched in the same query (no N+1). */
    @Override
    @EntityGraph(attributePaths = {"rawIntake"})
    List<Batch> findAll();
}