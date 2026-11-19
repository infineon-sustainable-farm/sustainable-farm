package com.infineonbit.sustainablefarm.modules.producttransformation.washsortrecord.repository;

import com.infineonbit.sustainablefarm.modules.producttransformation.washsortrecord.model.WashSortRecord;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
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
public interface WashSortRecordRepository extends JpaRepository<WashSortRecord, Long> {

    /** The WashSortRecord of a business code, shown as {@code recordId}. */
    @EntityGraph(attributePaths = {"batch", "operator"})
    Optional<WashSortRecord> findByRecordId(String recordId);

    /** Whether a WashSortRecord has this business code. */
    boolean existsByRecordId(String recordId);

    /** Deletes the row of a business code, with what it cascades to. */
    void deleteByRecordId(String recordId);


    /**
     * Find wash sort records by batch
     */
    @EntityGraph(attributePaths = {"batch", "operator"})
    List<WashSortRecord> findByBatchBatchId(String batchId);

    /**
     * Find wash sort records by date range
     */
    @EntityGraph(attributePaths = {"batch", "operator"})
    List<WashSortRecord> findByStartTimeBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find wash sort records by equipment
     */
    @EntityGraph(attributePaths = {"batch", "operator"})
    List<WashSortRecord> findByEquipmentId(Long equipmentId);

    /**
     * Find wash sort records by operator
     */
    @EntityGraph(attributePaths = {"batch", "operator"})
    List<WashSortRecord> findByOperatorOperatorId(String operatorId);

    /**
     * Find wash sort records by batch and date range
     */
    @Query("SELECT w FROM WashSortRecord w WHERE w.batch.batchId = :batchId AND w.startTime BETWEEN :startDate AND :endDate")
    @EntityGraph(attributePaths = {"batch", "operator"})
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
     * Aggregate input quantity by date range (SQL-side aggregation); returns 0 for empty periods.
     */
    @Query("SELECT COALESCE(SUM(w.inputQuantityKg), 0) FROM WashSortRecord w WHERE w.startTime BETWEEN :startDate AND :endDate")
    java.math.BigDecimal getTotalInputQuantityByDateRange(@Param("startDate") LocalDateTime startDate,
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
    @EntityGraph(attributePaths = {"batch", "operator"})
    List<WashSortRecord> findLowYieldRecords(@Param("threshold") Double threshold);

    /** Every row, with what the responses show fetched in the same query (no N+1). */
    @Override
    @EntityGraph(attributePaths = {"batch", "operator"})
    List<WashSortRecord> findAll();
}