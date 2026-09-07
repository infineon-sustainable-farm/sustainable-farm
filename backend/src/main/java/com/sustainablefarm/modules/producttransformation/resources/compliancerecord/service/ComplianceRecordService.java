package com.sustainablefarm.modules.producttransformation.resources.compliancerecord.service;

import com.sustainablefarm.modules.producttransformation.resources.compliancerecord.model.ComplianceRecord;
import com.sustainablefarm.modules.producttransformation.resources.compliancerecord.model.ComplianceRecord.ComplianceResult;
import com.sustainablefarm.modules.producttransformation.resources.compliancerecord.model.ComplianceRecord.ComplianceType;

import java.time.LocalDate;
import java.util.List;

/**
 * Service Interface for ComplianceRecord Entity
 * HACCP compliance data - Core Processing Entity
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
public interface ComplianceRecordService {

    /**
     * Create new compliance record
     */
    ComplianceRecord createComplianceRecord(ComplianceRecord complianceRecord);

    /**
     * Update compliance record
     */
    ComplianceRecord updateComplianceRecord(String recordId, ComplianceRecord complianceRecord);

    /**
     * Delete compliance record
     */
    void deleteComplianceRecord(String recordId);

    /**
     * Get compliance record by ID
     */
    ComplianceRecord getComplianceRecordById(String recordId);

    /**
     * Get all compliance records
     */
    List<ComplianceRecord> getAllComplianceRecords();

    /**
     * Get compliance records by batch
     */
    List<ComplianceRecord> getComplianceRecordsByBatch(String batchId);

    /**
     * Get compliance records by compliance type
     */
    List<ComplianceRecord> getComplianceRecordsByType(ComplianceType type);

    /**
     * Get compliance records by result
     */
    List<ComplianceRecord> getComplianceRecordsByResult(ComplianceResult result);

    /**
     * Get compliance records by auditor
     */
    List<ComplianceRecord> getComplianceRecordsByAuditor(String auditorId);

    /**
     * Get compliance records by audit date range
     */
    List<ComplianceRecord> getComplianceRecordsByDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * Get compliance records by batch and type
     */
    List<ComplianceRecord> getComplianceRecordsByBatchAndType(String batchId, ComplianceType type);

    /**
     * Get compliance records by batch and result
     */
    List<ComplianceRecord> getComplianceRecordsByBatchAndResult(String batchId, ComplianceResult result);

    /**
     * Get overdue compliance records
     * Business Rule: Next audit date tracking for compliance
     */
    List<ComplianceRecord> getOverdueAudits();

    /**
     * Count compliant records by batch
     */
    long countCompliantByBatch(String batchId);

    /**
     * Count non-compliant records by batch
     */
    long countNonCompliantByBatch(String batchId);

    /**
     * Get compliance records by type and result
     */
    List<ComplianceRecord> getComplianceRecordsByTypeAndResult(ComplianceType type, ComplianceResult result);

    /**
     * Check if batch has specific compliance type
     */
    boolean hasComplianceType(String batchId, ComplianceType type);

    /**
     * Get pending compliance records
     */
    List<ComplianceRecord> getPendingComplianceRecords();

    /**
     * Schedule next audit for compliance record
     * Business Rule: Next audit date tracking for compliance
     */
    ComplianceRecord scheduleNextAudit(String recordId, LocalDate nextAuditDate);

    /**
     * Complete compliance audit with result
     * Business Rule: Auditors must have AUDITOR role
     */
    ComplianceRecord completeAudit(String recordId, ComplianceResult result, String evidence);
}