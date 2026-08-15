package com.sustainablefarm.service;

import com.sustainablefarm.model.Batch;
import com.sustainablefarm.model.Batch.BatchStatus;
import com.sustainablefarm.model.HarvestEvent.MangoVariety;
import com.sustainablefarm.repository.BatchRepository;
import com.sustainablefarm.service.impl.BatchServiceImpl;
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
 * Unit tests for BatchService
 * 
 * Tests for:
 * - Valid Batch transition
 * - Invalid Batch transition
 * - Missing mandatory QC checkpoint
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class BatchServiceTest {

    @Mock
    private BatchRepository batchRepository;
    
    @Mock
    private QcCheckpointService qcCheckpointService;
    
    @InjectMocks
    private BatchServiceImpl batchService;
    
    private Batch testBatch;
    
    @BeforeEach
    void setUp() {
        testBatch = new Batch();
        testBatch.setBatchId("B-2026-001");
        testBatch.setHarvestDate(LocalDate.of(2026, 8, 15));
        testBatch.setMangoVariety(MangoVariety.KEITT);
        testBatch.setHarvestQuantityKg(1000.0);
        testBatch.setFarmId("FARM-001");
        testBatch.setBlockId("BLOCK-001");
        testBatch.setCurrentStatus(BatchStatus.CREATED);
    }
    
    @Test
    void testValidBatchTransition() {
        // Given
        when(batchRepository.findById("B-2026-001")).thenReturn(java.util.Optional.of(testBatch));
        when(batchRepository.save(any(Batch.class))).thenReturn(testBatch);
        
        // When
        Batch result = batchService.advanceBatchStatus("B-2026-001");
        
        // Then
        assertEquals(BatchStatus.INTAKE, result.getCurrentStatus());
        verify(batchRepository).save(testBatch);
    }
    
    @Test
    void testInvalidBatchTransition() {
        // Given
        testBatch.setCurrentStatus(BatchStatus.SHIPPED);
        when(batchRepository.findById("B-2026-001")).thenReturn(java.util.Optional.of(testBatch));
        
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> batchService.advanceBatchStatus("B-2026-001")
        );
        
        assertTrue(exception.getMessage().contains("Cannot advance batch status"));
    }
    
    @Test
    void testMissingMandatoryQCCheckpoint() {
        // Given
        testBatch.setCurrentStatus(BatchStatus.WASHING);
        when(batchRepository.findById("B-2026-001")).thenReturn(java.util.Optional.of(testBatch));
        when(qcCheckpointService.countByBatchAndStage("B-2026-001", com.sustainablefarm.model.QcCheckpoint.QcStage.WASHING)).thenReturn(0L);
        
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> batchService.advanceBatchStatus("B-2026-001")
        );
        
        assertTrue(exception.getMessage().contains("mandatory WASHING QC checkpoint not completed"));
    }
    
    @Test
    void testMissingCoolingQCCheckpoint() {
        // Given
        testBatch.setCurrentStatus(BatchStatus.DRYING);
        when(batchRepository.findById("B-2026-001")).thenReturn(java.util.Optional.of(testBatch));
        when(qcCheckpointService.countByBatchAndStage("B-2026-001", com.sustainablefarm.model.QcCheckpoint.QcStage.COOLING)).thenReturn(0L);
        
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> batchService.advanceBatchStatus("B-2026-001")
        );
        
        assertTrue(exception.getMessage().contains("mandatory COOLING QC checkpoint not completed"));
    }
    
    @Test
    void testCreateBatchWithDefaultStatus() {
        // Given
        Batch newBatch = new Batch();
        newBatch.setBatchId("B-2026-002");
        when(batchRepository.save(any(Batch.class))).thenReturn(newBatch);
        
        // When
        Batch result = batchService.createBatch(newBatch);
        
        // Then
        assertEquals(BatchStatus.CREATED, result.getCurrentStatus());
        verify(batchRepository).save(newBatch);
    }
    
    @Test
    void testSetBatchStatusDirectly() {
        // Given
        when(batchRepository.findById("B-2026-001")).thenReturn(java.util.Optional.of(testBatch));
        when(batchRepository.save(any(Batch.class))).thenReturn(testBatch);
        
        // When
        Batch result = batchService.setBatchStatus("B-2026-001", BatchStatus.REJECTED);
        
        // Then
        assertEquals(BatchStatus.REJECTED, result.getCurrentStatus());
        verify(batchRepository).save(testBatch);
    }
}