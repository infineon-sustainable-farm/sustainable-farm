package com.sustainablefarm.modules.producttransformation.resources.batch.repository;

import com.sustainablefarm.modules.producttransformation.resources.batch.model.Batch;
import com.sustainablefarm.modules.producttransformation.resources.batch.model.Batch.BatchStatus;
import com.sustainablefarm.modules.producttransformation.resources.harvestevent.model.HarvestEvent.MangoVariety;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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
public interface BatchRepository extends JpaRepository<Batch, String> {

    /**
     * Find batches by current status
     */
    List<Batch> findByCurrentStatus(BatchStatus currentStatus);

    /**
     * Find batches by harvest date range
     */
    List<Batch> findByHarvestDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find batches by mango variety
     */
    List<Batch> findByMangoVariety(MangoVariety mangoVariety);

    /**
     * Find batches by farm
     */
    List<Batch> findByFarmId(String farmId);

    /**
     * Find batches by farm and status
     */
    @Query("SELECT b FROM Batch b WHERE b.farmId = :farmId AND b.currentStatus = :status")
    List<Batch> findByFarmAndStatus(@Param("farmId") String farmId, @Param("status") BatchStatus status);

    /**
     * Find batches by variety and status
     */
    @Query("SELECT b FROM Batch b WHERE b.mangoVariety = :variety AND b.currentStatus = :status")
    List<Batch> findByVarietyAndStatus(@Param("variety") MangoVariety variety, @Param("status") BatchStatus status);

    /**
     * Count batches by status
     */
    @Query("SELECT COUNT(b) FROM Batch b WHERE b.currentStatus = :status")
    long countByStatus(@Param("status") BatchStatus status);

    /**
     * Get total harvest quantity by variety
     */
    @Query("SELECT SUM(b.harvestQuantityKg) FROM Batch b WHERE b.mangoVariety = :variety")
    Double getTotalHarvestQuantityByVariety(@Param("variety") MangoVariety variety);

    /**
     * Find batches ready for shipping
     */
    @Query("SELECT b FROM Batch b WHERE b.currentStatus = 'COMPLETED'")
    List<Batch> findReadyForShipping();

    /**
     * Find batches by block
     */
    List<Batch> findByBlockId(String blockId);

    /**
     * Find batches by farm and block
     */
    @Query("SELECT b FROM Batch b WHERE b.farmId = :farmId AND b.blockId = :blockId")
    List<Batch> findByFarmAndBlock(@Param("farmId") String farmId, @Param("blockId") String blockId);

    /**
     * Check if batch exists by status
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Batch b WHERE b.batchId = :id AND b.currentStatus = :status")
    boolean existsByBatchIdAndCurrentStatus(@Param("id") String batchId, @Param("status") BatchStatus status);
}