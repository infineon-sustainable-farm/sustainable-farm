package com.infineonbit.sustainablefarm.modules.producttransformation.compliancerecord.repository;

import com.infineonbit.sustainablefarm.modules.producttransformation.compliancerecord.model.ComplianceRecord;
import com.infineonbit.sustainablefarm.modules.producttransformation.compliancerecord.model.ComplianceResult;
import com.infineonbit.sustainablefarm.modules.producttransformation.compliancerecord.model.ComplianceType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for ComplianceRecord entity
 * HACCP compliance data - Core Processing Entity
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Repository
public interface ComplianceRecordRepository extends JpaRepository<ComplianceRecord, Long> {

    /** The ComplianceRecord of a business code, shown as {@code recordId}. */
    @EntityGraph(attributePaths = {"batch", "auditor"})
    Optional<ComplianceRecord> findByRecordId(String recordId);

    /** Whether a ComplianceRecord has this business code. */
    boolean existsByRecordId(String recordId);

    /** Deletes the row of a business code, with what it cascades to. */
    void deleteByRecordId(String recordId);


    /**
     * Find compliance records by batch
     */
    @EntityGraph(attributePaths = {"batch", "auditor"})
    List<ComplianceRecord> findByBatchBatchId(String batchId);

    /**
     * Find compliance records by compliance type
     */
    @EntityGraph(attributePaths = {"batch", "auditor"})
    List<ComplianceRecord> findByComplianceType(ComplianceType complianceType);

    /**
     * Find compliance records by result
     */
    @EntityGraph(attributePaths = {"batch", "auditor"})
    List<ComplianceRecord> findByResult(ComplianceResult result);

    /**
     * Find compliance records by auditor
     */
    @EntityGraph(attributePaths = {"batch", "auditor"})
    List<ComplianceRecord> findByAuditorOperatorId(String auditorId);

    /**
     * Find compliance records by audit date range
     */
    @EntityGraph(attributePaths = {"batch", "auditor"})
    List<ComplianceRecord> findByAuditDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find compliance records by batch and type
     */
    @Query("SELECT c FROM ComplianceRecord c WHERE c.batch.batchId = :batchId AND c.complianceType = :type")
    @EntityGraph(attributePaths = {"batch", "auditor"})
    List<ComplianceRecord> findByBatchAndType(@Param("batchId") String batchId, @Param("type") ComplianceType type);

    /**
     * Find compliance records by batch and result
     */
    @Query("SELECT c FROM ComplianceRecord c WHERE c.batch.batchId = :batchId AND c.result = :result")
    @EntityGraph(attributePaths = {"batch", "auditor"})
    List<ComplianceRecord> findByBatchAndResult(@Param("batchId") String batchId, @Param("result") ComplianceResult result);

    /**
     * Find overdue compliance records
     * Business Rule: Next audit date tracking for compliance
     */
    @Query("SELECT c FROM ComplianceRecord c WHERE c.nextAuditDate < :date")
    @EntityGraph(attributePaths = {"batch", "auditor"})
    List<ComplianceRecord> findOverdueAudits(@Param("date") LocalDate date);

    /**
     * Find compliance records by batch and date range
     */
    @Query("SELECT c FROM ComplianceRecord c WHERE c.batch.batchId = :batchId AND c.auditDate BETWEEN :startDate AND :endDate")
    @EntityGraph(attributePaths = {"batch", "auditor"})
    List<ComplianceRecord> findByBatchAndDateRange(@Param("batchId") String batchId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    /**
     * Count compliant records by batch
     */
    @Query("SELECT COUNT(c) FROM ComplianceRecord c WHERE c.batch.batchId = :batchId AND c.result = 'COMPLIANT'")
    long countCompliantByBatch(@Param("batchId") String batchId);

    /**
     * Count non-compliant records by batch
     */
    @Query("SELECT COUNT(c) FROM ComplianceRecord c WHERE c.batch.batchId = :batchId AND c.result = 'NON_COMPLIANT'")
    long countNonCompliantByBatch(@Param("batchId") String batchId);

    /**
     * Find compliance records by type and result
     */
    @Query("SELECT c FROM ComplianceRecord c WHERE c.complianceType = :type AND c.result = :result")
    @EntityGraph(attributePaths = {"batch", "auditor"})
    List<ComplianceRecord> findByTypeAndResult(@Param("type") ComplianceType type, @Param("result") ComplianceResult result);

    /**
     * Check if batch has specific compliance type
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM ComplianceRecord c WHERE c.batch.batchId = :batchId AND c.complianceType = :type")
    boolean hasComplianceType(@Param("batchId") String batchId, @Param("type") ComplianceType type);

    /**
     * Find pending compliance records
     */
    @Query("SELECT c FROM ComplianceRecord c WHERE c.result = 'PENDING'")
    @EntityGraph(attributePaths = {"batch", "auditor"})
    List<ComplianceRecord> findPendingComplianceRecords();

    /** Every row, with what the responses show fetched in the same query (no N+1). */
    @Override
    @EntityGraph(attributePaths = {"batch", "auditor"})
    List<ComplianceRecord> findAll();
}