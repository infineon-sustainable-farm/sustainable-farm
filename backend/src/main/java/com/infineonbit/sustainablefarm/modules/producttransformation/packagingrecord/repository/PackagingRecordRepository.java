package com.infineonbit.sustainablefarm.modules.producttransformation.packagingrecord.repository;

import com.infineonbit.sustainablefarm.modules.producttransformation.packagingrecord.model.PackagingRecord;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for PackagingRecord entity
 * Packaging stage data - Core Processing Entity
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Repository
public interface PackagingRecordRepository extends JpaRepository<PackagingRecord, Long> {

    /** The PackagingRecord of a business code, shown as {@code recordId}. */
    @EntityGraph(attributePaths = {"batch", "operator"})
    Optional<PackagingRecord> findByRecordId(String recordId);

    /** Whether a PackagingRecord has this business code. */
    boolean existsByRecordId(String recordId);

    /** Deletes the row of a business code, with what it cascades to. */
    void deleteByRecordId(String recordId);


    /**
     * Find packaging records by batch
     */
    @EntityGraph(attributePaths = {"batch", "operator"})
    List<PackagingRecord> findByBatchBatchId(String batchId);

    /**
     * Find packaging records by date range
     */
    @EntityGraph(attributePaths = {"batch", "operator"})
    List<PackagingRecord> findByPackagingDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find packaging records by package type
     */
    @Query("SELECT p FROM PackagingRecord p WHERE p.packageType = :type")
    @EntityGraph(attributePaths = {"batch", "operator"})
    List<PackagingRecord> findByPackageType(@Param("type") String type);

    /**
     * Find packaging records by equipment
     */
    @EntityGraph(attributePaths = {"batch", "operator"})
    List<PackagingRecord> findByEquipmentId(Long equipmentId);

    /**
     * Find packaging records by operator
     */
    @EntityGraph(attributePaths = {"batch", "operator"})
    List<PackagingRecord> findByOperatorOperatorId(String operatorId);

    /**
     * Find packaging records by lot code
     */
    @EntityGraph(attributePaths = {"batch", "operator"})
    List<PackagingRecord> findByLotCode(String lotCode);

    /**
     * Find export-ready packaging records
     * Business Rule: Export ready flag for EU compliance
     */
    @EntityGraph(attributePaths = {"batch", "operator"})
    List<PackagingRecord> findByExportReadyTrue();

    /**
     * Find packaging records by batch and date range
     */
    @Query("SELECT p FROM PackagingRecord p WHERE p.batch.batchId = :batchId AND p.packagingDate BETWEEN :startDate AND :endDate")
    @EntityGraph(attributePaths = {"batch", "operator"})
    List<PackagingRecord> findByBatchAndDateRange(@Param("batchId") String batchId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    /**
     * Check if lot code exists
     * Business Rule: Lot codes must be unique for traceability
     */
    boolean existsByLotCode(String lotCode);

    /**
     * Get total packaged quantity by package type
     */
    @Query("SELECT SUM(p.packageQuantityKg) FROM PackagingRecord p WHERE p.packageType = :type")
    Double getTotalPackagedQuantityByType(@Param("type") String type);

    /**
     * Find packaging records ready for export by date
     */
    @Query("SELECT p FROM PackagingRecord p WHERE p.exportReady = true AND p.packagingDate >= :date")
    @EntityGraph(attributePaths = {"batch", "operator"})
    List<PackagingRecord> findExportReadyByDate(@Param("date") LocalDate date);

    /**
     * Count export-ready batches
     */
    @Query("SELECT COUNT(DISTINCT p.batch.batchId) FROM PackagingRecord p WHERE p.exportReady = true")
    long countExportReadyBatches();

    /** Every row, with what the responses show fetched in the same query (no N+1). */
    @Override
    @EntityGraph(attributePaths = {"batch", "operator"})
    List<PackagingRecord> findAll();
}