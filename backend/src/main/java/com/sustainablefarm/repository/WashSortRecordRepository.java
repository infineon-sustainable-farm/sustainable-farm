package com.sustainablefarm.repository;

import com.sustainablefarm.model.WashSortRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for WashSortRecord entity
 * Washing and sorting stage data - Core Processing Entity
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Repository
public interface WashSortRecordRepository extends JpaRepository<WashSortRecord, String> {

    /**
     * Find wash sort records by batch
     */
    List<WashSortRecord> findByBatchBatchId(String batchId);

    /**
     * Find wash sort records by date range
     */
    List<WashSortRecord> findByStartTimeBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find wash sort records by equipment
     */
    List<WashSortRecord> findByEquipmentEquipmentId(String equipmentId);

    /**
     * Find wash sort records by operator
     */
    List<WashSortRecord> findByOperatorOperatorId(String operatorId);

    /**
     * Find wash sort records by batch and date range
     */
    @Query("SELECT w FROM WashSortRecord w WHERE w.batch.batchId = :batchId AND w.startTime BETWEEN :startDate AND :endDate")
    List<WashSortRecord> findByBatchAndDateRange(@Param("batchId") String batchId,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    /**
     * Get total water usage by date range
     */
    @Query("SELECT SUM(w.waterUsageLiters) FROM WashSortRecord w WHERE w.startTime BETWEEN :startDate AND :endDate")
    Double getTotalWaterUsageByDateRange(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    /**
     * Get average yield percentage by batch
     */
    @Query("SELECT AVG((w.outputQuantityKg / w.inputQuantityKg) * 100) FROM WashSortRecord w WHERE w.batch.batchId = :batchId")
    Double getAverageYieldPercentageByBatch(@Param("batchId") String batchId);

    /**
     * Get total waste by batch
     */
    @Query("SELECT SUM(w.wasteQuantityKg) FROM WashSortRecord w WHERE w.batch.batchId = :batchId")
    Double getTotalWasteByBatch(@Param("batchId") String batchId);

    /**
     * Find wash sort records with yield below threshold
     */
    @Query("SELECT w FROM WashSortRecord w WHERE (w.outputQuantityKg / w.inputQuantityKg) * 100 < :threshold")
    List<WashSortRecord> findLowYieldRecords(@Param("threshold") Double threshold);
}