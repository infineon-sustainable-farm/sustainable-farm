package com.sustainablefarm.modules.producttransformation.resources.compliancerecord.repository;

import com.sustainablefarm.modules.producttransformation.resources.compliancerecord.model.ComplianceRecord;
import com.sustainablefarm.modules.producttransformation.resources.compliancerecord.model.ComplianceRecord.ComplianceResult;
import com.sustainablefarm.modules.producttransformation.resources.compliancerecord.model.ComplianceRecord.ComplianceType;
import org.springframework.data.jpa.repository.JpaRepository;
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
public interface ComplianceRecordRepository extends JpaRepository<ComplianceRecord, String> {

    /**
     * Find compliance records by batch
     */
    List<ComplianceRecord> findByBatchBatchId(String batchId);

    /**
     * Find compliance records by compliance type
     */
    List<ComplianceRecord> findByComplianceType(ComplianceType complianceType);

    /**
     * Find compliance records by result
     */
    List<ComplianceRecord> findByResult(ComplianceResult result);

    /**
     * Find compliance records by auditor
     */
    List<ComplianceRecord> findByAuditorOperatorId(String auditorId);

    /**
     * Find compliance records by audit date range
     */
    List<ComplianceRecord> findByAuditDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find compliance records by batch and type
     */
    @Query("SELECT c FROM ComplianceRecord c WHERE c.batch.batchId = :batchId AND c.complianceType = :type")
    List<ComplianceRecord> findByBatchAndType(@Param("batchId") String batchId, @Param("type") ComplianceType type);

    /**
     * Find compliance records by batch and result
     */
    @Query("SELECT c FROM ComplianceRecord c WHERE c.batch.batchId = :batchId AND c.result = :result")
    List<ComplianceRecord> findByBatchAndResult(@Param("batchId") String batchId, @Param("result") ComplianceResult result);

    /**
     * Find overdue compliance records
     * Business Rule: Next audit date tracking for compliance
     */
    @Query("SELECT c FROM ComplianceRecord c WHERE c.nextAuditDate < :date")
    List<ComplianceRecord> findOverdueAudits(@Param("date") LocalDate date);

    /**
     * Find compliance records by batch and date range
     */
    @Query("SELECT c FROM ComplianceRecord c WHERE c.batch.batchId = :batchId AND c.auditDate BETWEEN :startDate AND :endDate")
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
    List<ComplianceRecord> findPendingComplianceRecords();
}