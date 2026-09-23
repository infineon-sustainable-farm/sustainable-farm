package com.sustainablefarm.modules.producttransformation.resources.compliancerecord.impl;

import com.sustainablefarm.modules.producttransformation.resources.batch.model.Batch;
import com.sustainablefarm.modules.producttransformation.resources.compliancerecord.model.ComplianceRecord;
import com.sustainablefarm.modules.producttransformation.resources.compliancerecord.model.ComplianceRecord.ComplianceResult;
import com.sustainablefarm.modules.producttransformation.resources.compliancerecord.model.ComplianceRecord.ComplianceType;
import com.sustainablefarm.modules.producttransformation.resources.operator.model.Operator;
import com.sustainablefarm.modules.producttransformation.resources.batch.repository.BatchRepository;
import com.sustainablefarm.modules.producttransformation.resources.compliancerecord.repository.ComplianceRecordRepository;
import com.sustainablefarm.modules.producttransformation.resources.operator.repository.OperatorRepository;
import com.sustainablefarm.modules.producttransformation.resources.compliancerecord.service.ComplianceRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service Implementation for ComplianceRecord Entity
 * HACCP compliance data - Core Processing Entity
 * 
 * Business Rules:
 * - Mandatory for HACCP certification
 * - Auditors must have AUDITOR role
 * - Result must be COMPLIANT for HACCP certification
 * - Next audit date tracking for compliance
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Service
@Transactional
public class ComplianceRecordServiceImpl implements ComplianceRecordService {

    private final ComplianceRecordRepository complianceRecordRepository;
    private final BatchRepository batchRepository;
    private final OperatorRepository operatorRepository;

    @Autowired
    public ComplianceRecordServiceImpl(ComplianceRecordRepository complianceRecordRepository,
                                      BatchRepository batchRepository,
                                      OperatorRepository operatorRepository) {
        this.complianceRecordRepository = complianceRecordRepository;
        this.batchRepository = batchRepository;
        this.operatorRepository = operatorRepository;
    }

    @Override
    public ComplianceRecord createComplianceRecord(ComplianceRecord complianceRecord) {
        // Business Rule: Validate auditor is certified
        if (complianceRecord.getAuditor() != null) {
            Operator auditor = operatorRepository.findById(complianceRecord.getAuditor().getOperatorId())
                    .orElseThrow(() -> new IllegalArgumentException("Auditor not found"));
            
            if (auditor.getRole() != Operator.Role.AUDITOR) {
                throw new IllegalArgumentException(
                    "Auditor must have AUDITOR role. Current role: " + auditor.getRole()
                );
            }
        }
        
        return complianceRecordRepository.save(complianceRecord);
    }

    @Override
    public ComplianceRecord updateComplianceRecord(String recordId, ComplianceRecord complianceRecord) {
        ComplianceRecord existingRecord = getComplianceRecordById(recordId);
        
        // Update fields
        existingRecord.setComplianceType(complianceRecord.getComplianceType());
        existingRecord.setRequirement(complianceRecord.getRequirement());
        existingRecord.setResult(complianceRecord.getResult());
        existingRecord.setEvidence(complianceRecord.getEvidence());
        existingRecord.setAuditor(complianceRecord.getAuditor());
        existingRecord.setAuditDate(complianceRecord.getAuditDate());
        existingRecord.setNextAuditDate(complianceRecord.getNextAuditDate());
        
        return complianceRecordRepository.save(existingRecord);
    }

    @Override
    public void deleteComplianceRecord(String recordId) {
        if (!complianceRecordRepository.existsById(recordId)) {
            throw new IllegalArgumentException("Compliance record not found with ID: " + recordId);
        }
        complianceRecordRepository.deleteById(recordId);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceRecord getComplianceRecordById(String recordId) {
        return complianceRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Compliance record not found with ID: " + recordId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceRecord> getAllComplianceRecords() {
        return complianceRecordRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceRecord> getComplianceRecordsByBatch(String batchId) {
        return complianceRecordRepository.findByBatchBatchId(batchId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceRecord> getComplianceRecordsByType(ComplianceType type) {
        return complianceRecordRepository.findByComplianceType(type);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceRecord> getComplianceRecordsByResult(ComplianceResult result) {
        return complianceRecordRepository.findByResult(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceRecord> getComplianceRecordsByAuditor(String auditorId) {
        return complianceRecordRepository.findByAuditorOperatorId(auditorId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceRecord> getComplianceRecordsByDateRange(LocalDate startDate, LocalDate endDate) {
        return complianceRecordRepository.findByAuditDateBetween(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceRecord> getComplianceRecordsByBatchAndType(String batchId, ComplianceType type) {
        return complianceRecordRepository.findByBatchAndType(batchId, type);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceRecord> getComplianceRecordsByBatchAndResult(String batchId, ComplianceResult result) {
        return complianceRecordRepository.findByBatchAndResult(batchId, result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceRecord> getOverdueAudits() {
        // Business Rule: Next audit date tracking for compliance
        return complianceRecordRepository.findOverdueAudits(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public long countCompliantByBatch(String batchId) {
        return complianceRecordRepository.countCompliantByBatch(batchId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countNonCompliantByBatch(String batchId) {
        return complianceRecordRepository.countNonCompliantByBatch(batchId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceRecord> getComplianceRecordsByTypeAndResult(ComplianceType type, ComplianceResult result) {
        return complianceRecordRepository.findByTypeAndResult(type, result);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasComplianceType(String batchId, ComplianceType type) {
        return complianceRecordRepository.hasComplianceType(batchId, type);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceRecord> getPendingComplianceRecords() {
        return complianceRecordRepository.findPendingComplianceRecords();
    }

    @Override
    public ComplianceRecord scheduleNextAudit(String recordId, LocalDate nextAuditDate) {
        ComplianceRecord record = getComplianceRecordById(recordId);
        
        // Business Rule: Next audit date tracking for compliance
        record.setNextAuditDate(nextAuditDate);
        
        return complianceRecordRepository.save(record);
    }

    @Override
    public ComplianceRecord completeAudit(String recordId, ComplianceResult result, String evidence) {
        ComplianceRecord record = getComplianceRecordById(recordId);
        
        // Business Rule: Auditors must have AUDITOR role (validated at entity level)
        record.setResult(result);
        record.setEvidence(evidence);
        record.setAuditDate(LocalDate.now());
        
        // If compliant, schedule next audit
        if (result == ComplianceResult.COMPLIANT) {
            // Schedule next audit in 6 months
            record.setNextAuditDate(LocalDate.now().plusMonths(6));
        }
        
        return complianceRecordRepository.save(record);
    }
}