package com.sustainablefarm.service;

import com.sustainablefarm.model.ComplianceRecord;
import com.sustainablefarm.model.ComplianceRecord.ComplianceResult;
import com.sustainablefarm.model.ComplianceRecord.ComplianceType;
import com.sustainablefarm.model.Operator;
import com.sustainablefarm.model.Operator.Role;
import com.sustainablefarm.model.Batch;
import com.sustainablefarm.repository.ComplianceRecordRepository;
import com.sustainablefarm.repository.BatchRepository;
import com.sustainablefarm.repository.OperatorRepository;
import com.sustainablefarm.service.impl.ComplianceRecordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ComplianceRecordService
 * 
 * Tests for:
 * - Unauthorized HACCP operation
 * - Auditor role validation
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class ComplianceRecordServiceTest {

    @Mock
    private ComplianceRecordRepository complianceRecordRepository;
    
    @Mock
    private BatchRepository batchRepository;
    
    @Mock
    private OperatorRepository operatorRepository;
    
    @InjectMocks
    private ComplianceRecordServiceImpl complianceRecordService;
    
    private ComplianceRecord testComplianceRecord;
    private Operator testOperator;
    
    @BeforeEach
    void setUp() {
        testComplianceRecord = new ComplianceRecord();
        testComplianceRecord.setRecordId("CR-001");
        testComplianceRecord.setComplianceType(ComplianceType.HACCP);
        testComplianceRecord.setRequirement("HACCP compliance check");
        testComplianceRecord.setResult(ComplianceResult.COMPLIANT);
        testComplianceRecord.setAuditDate(LocalDate.now());
        
        testOperator = new Operator();
        testOperator.setOperatorId("OP-001");
        testOperator.setRole(Role.AUDITOR);
        testOperator.setActiveStatus(com.sustainablefarm.model.Operator.ActiveStatus.ACTIVE);
    }
    
    @Test
    void testCreateComplianceRecord_UnauthorizedAuditor() {
        // Given
        testOperator.setRole(Role.WASHER); // Wrong role
        testComplianceRecord.setAuditor(testOperator);
        when(operatorRepository.findById("OP-001")).thenReturn(java.util.Optional.of(testOperator));
        
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> complianceRecordService.createComplianceRecord(testComplianceRecord)
        );
        
        assertTrue(exception.getMessage().contains("Auditor must have AUDITOR role"));
    }
    
    @Test
    void testCreateComplianceRecord_AuthorizedAuditor() {
        // Given
        testComplianceRecord.setAuditor(testOperator);
        when(operatorRepository.findById("OP-001")).thenReturn(java.util.Optional.of(testOperator));
        when(complianceRecordRepository.save(any(ComplianceRecord.class))).thenReturn(testComplianceRecord);
        
        // When
        ComplianceRecord result = complianceRecordService.createComplianceRecord(testComplianceRecord);
        
        // Then
        assertEquals(testComplianceRecord, result);
        verify(complianceRecordRepository).save(testComplianceRecord);
    }
    
    @Test
    void testCompleteAudit_Compliant() {
        // Given
        when(complianceRecordRepository.findById("CR-001")).thenReturn(java.util.Optional.of(testComplianceRecord));
        when(complianceRecordRepository.save(any(ComplianceRecord.class))).thenReturn(testComplianceRecord);
        
        // When
        ComplianceRecord result = complianceRecordService.completeAudit("CR-001", ComplianceResult.COMPLIANT, "Evidence text");
        
        // Then
        assertEquals(ComplianceResult.COMPLIANT, result.getResult());
        assertEquals("Evidence text", result.getEvidence());
        assertNotNull(result.getNextAuditDate());
        verify(complianceRecordRepository).save(testComplianceRecord);
    }
    
    @Test
    void testHasComplianceType() {
        // Given
        when(complianceRecordRepository.hasComplianceType("B-001", ComplianceType.HACCP)).thenReturn(true);
        
        // When
        boolean result = complianceRecordService.hasComplianceType("B-001", ComplianceType.HACCP);
        
        // Then
        assertTrue(result);
        verify(complianceRecordRepository).hasComplianceType("B-001", ComplianceType.HACCP);
    }
    
    @Test
    void testGetOverdueAudits() {
        // Given
        when(complianceRecordRepository.findOverdueAudits(LocalDate.now())).thenReturn(java.util.List.of(testComplianceRecord));
        
        // When
        var result = complianceRecordService.getOverdueAudits();
        
        // Then
        assertEquals(1, result.size());
        verify(complianceRecordRepository).findOverdueAudits(LocalDate.now());
    }
    
    @Test
    void testCountCompliantByBatch() {
        // Given
        when(complianceRecordRepository.countCompliantByBatch("B-001")).thenReturn(5L);
        
        // When
        long result = complianceRecordService.countCompliantByBatch("B-001");
        
        // Then
        assertEquals(5L, result);
        verify(complianceRecordRepository).countCompliantByBatch("B-001");
    }
}