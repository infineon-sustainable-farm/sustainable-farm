package com.sustainablefarm.service;

import com.sustainablefarm.model.QcCheckpoint;
import com.sustainablefarm.model.QcCheckpoint.QcStage;
import com.sustainablefarm.model.QcCheckpoint.QcResult;
import com.sustainablefarm.model.Operator;
import com.sustainablefarm.model.Operator.Role;
import com.sustainablefarm.model.Batch;
import com.sustainablefarm.repository.QcCheckpointRepository;
import com.sustainablefarm.repository.BatchRepository;
import com.sustainablefarm.repository.OperatorRepository;
import com.sustainablefarm.service.impl.QcCheckpointServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for QcCheckpointService
 * 
 * Tests for:
 * - Missing mandatory QC checkpoint
 * - Unauthorized operator rejection
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class QcCheckpointServiceTest {

    @Mock
    private QcCheckpointRepository qcCheckpointRepository;
    
    @Mock
    private BatchRepository batchRepository;
    
    @Mock
    private OperatorRepository operatorRepository;
    
    @InjectMocks
    private QcCheckpointServiceImpl qcCheckpointService;
    
    private QcCheckpoint testQcCheckpoint;
    private Operator testOperator;
    
    @BeforeEach
    void setUp() {
        testQcCheckpoint = new QcCheckpoint();
        testQcCheckpoint.setCheckpointId("QC-001");
        testQcCheckpoint.setStage(QcStage.WASHING);
        testQcCheckpoint.setResult(QcResult.PASS);
        testQcCheckpoint.setDefectsCount(0);
        testQcCheckpoint.setCheckpointTime(LocalDateTime.now());
        
        testOperator = new Operator();
        testOperator.setOperatorId("OP-001");
        testOperator.setRole(Role.QC_INSPECTOR);
        testOperator.setActiveStatus(com.sustainablefarm.model.Operator.ActiveStatus.ACTIVE);
    }
    
    @Test
    void testHasMandatoryCheckpointsCompleted_BothPresent() {
        // Given
        when(qcCheckpointRepository.countByBatchAndStage("B-001", QcStage.WASHING)).thenReturn(1L);
        when(qcCheckpointRepository.countByBatchAndStage("B-001", QcStage.COOLING)).thenReturn(1L);
        
        // When
        boolean result = qcCheckpointService.hasMandatoryCheckpointsCompleted("B-001");
        
        // Then
        assertTrue(result);
        verify(qcCheckpointRepository).countByBatchAndStage("B-001", QcStage.WASHING);
        verify(qcCheckpointRepository).countByBatchAndStage("B-001", QcStage.COOLING);
    }
    
    @Test
    void testHasMandatoryCheckpointsCompleted_MissingWashing() {
        // Given
        when(qcCheckpointRepository.countByBatchAndStage("B-001", QcStage.WASHING)).thenReturn(0L);
        when(qcCheckpointRepository.countByBatchAndStage("B-001", QcStage.COOLING)).thenReturn(1L);
        
        // When
        boolean result = qcCheckpointService.hasMandatoryCheckpointsCompleted("B-001");
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testHasMandatoryCheckpointsCompleted_MissingCooling() {
        // Given
        when(qcCheckpointRepository.countByBatchAndStage("B-001", QcStage.WASHING)).thenReturn(1L);
        when(qcCheckpointRepository.countByBatchAndStage("B-001", QcStage.COOLING)).thenReturn(0L);
        
        // When
        boolean result = qcCheckpointService.hasMandatoryCheckpointsCompleted("B-001");
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testCreateQcCheckpoint_UnauthorizedOperator() {
        // Given
        testOperator.setRole(Role.WASHER); // Wrong role
        testQcCheckpoint.setInspector(testOperator);
        when(operatorRepository.findById("OP-001")).thenReturn(java.util.Optional.of(testOperator));
        
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> qcCheckpointService.createQcCheckpoint(testQcCheckpoint)
        );
        
        assertTrue(exception.getMessage().contains("Inspector must have QC_INSPECTOR role"));
    }
    
    @Test
    void testCreateMandatoryCheckpoint_ValidStage() {
        // Given
        when(batchRepository.findById("B-001")).thenReturn(java.util.Optional.of(new com.sustainablefarm.model.Batch()));
        when(operatorRepository.findById("OP-001")).thenReturn(java.util.Optional.of(testOperator));
        when(qcCheckpointRepository.save(any(QcCheckpoint.class))).thenReturn(testQcCheckpoint);
        
        // When
        QcCheckpoint result = qcCheckpointService.createMandatoryCheckpoint("B-001", QcStage.WASHING, "OP-001");
        
        // Then
        assertEquals(QcStage.WASHING, result.getStage());
        assertEquals(QcResult.PENDING, result.getResult());
        verify(qcCheckpointRepository).save(testQcCheckpoint);
    }
    
    @Test
    void testCreateMandatoryCheckpoint_InvalidStage() {
        // Given
        when(batchRepository.findById("B-001")).thenReturn(java.util.Optional.of(new com.sustainablefarm.model.Batch()));
        
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> qcCheckpointService.createMandatoryCheckpoint("B-001", QcStage.INTAKE, "OP-001")
        );
        
        assertTrue(exception.getMessage().contains("Stage must be WASHING or COOLING"));
    }
    
    @Test
    void testGetPassRateByStage() {
        // Given
        when(qcCheckpointRepository.countPassedByStage(QcStage.WASHING)).thenReturn(8L);
        when(qcCheckpointRepository.countTotalByStage(QcStage.WASHING)).thenReturn(10L);
        
        // When
        double result = qcCheckpointService.getPassRateByStage(QcStage.WASHING);
        
        // Then
        assertEquals(80.0, result);
        verify(qcCheckpointRepository).countPassedByStage(QcStage.WASHING);
        verify(qcCheckpointRepository).countTotalByStage(QcStage.WASHING);
    }
}