package com.sustainablefarm.modules.producttransformation.resources.packagingrecord.service;

import com.sustainablefarm.modules.producttransformation.resources.packagingrecord.model.PackagingRecord;
import com.sustainablefarm.modules.producttransformation.resources.packagingrecord.model.PackagingRecord.PackageType;
import com.sustainablefarm.modules.producttransformation.resources.packagingrecord.repository.PackagingRecordRepository;
import com.sustainablefarm.modules.producttransformation.resources.packagingrecord.impl.PackagingRecordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PackagingRecordService
 * 
 * Tests for:
 * - Duplicate lot code
 * - Lot code validation
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class PackagingRecordServiceTest {

    @Mock
    private PackagingRecordRepository packagingRecordRepository;
    
    @InjectMocks
    private PackagingRecordServiceImpl packagingRecordService;
    
    private PackagingRecord testPackagingRecord;
    
    @BeforeEach
    void setUp() {
        testPackagingRecord = new PackagingRecord();
        testPackagingRecord.setRecordId("PR-001");
        testPackagingRecord.setPackageType(PackagingRecord.PackageType.ONE_KG_BAG);
        testPackagingRecord.setPackageQuantityKg(new java.math.BigDecimal("10.0"));
        testPackagingRecord.setLotCode("LOT-2026-001");
        testPackagingRecord.setExportReady(false);
        testPackagingRecord.setPackagingDate(LocalDate.now());
    }
    
    @Test
    void testLotCodeExists() {
        // Given
        when(packagingRecordRepository.existsByLotCode("LOT-2026-001")).thenReturn(true);
        
        // When
        boolean result = packagingRecordService.lotCodeExists("LOT-2026-001");
        
        // Then
        assertTrue(result);
        verify(packagingRecordRepository).existsByLotCode("LOT-2026-001");
    }
    
    @Test
    void testLotCodeDoesNotExist() {
        // Given
        when(packagingRecordRepository.existsByLotCode("LOT-2026-002")).thenReturn(false);
        
        // When
        boolean result = packagingRecordService.lotCodeExists("LOT-2026-002");
        
        // Then
        assertFalse(result);
        verify(packagingRecordRepository).existsByLotCode("LOT-2026-002");
    }
    
    @Test
    void testCreatePackagingRecord_DuplicateLotCode() {
        // Given
        when(packagingRecordRepository.existsByLotCode("LOT-2026-001")).thenReturn(true);
        
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> packagingRecordService.createPackagingRecord(testPackagingRecord)
        );
        
        assertTrue(exception.getMessage().contains("Lot code already exists"));
    }
    
    @Test
    void testCreatePackagingRecord_EmptyLotCode() {
        // Given
        testPackagingRecord.setLotCode("");
        when(packagingRecordRepository.existsByLotCode("")).thenReturn(false);
        
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> packagingRecordService.createPackagingRecord(testPackagingRecord)
        );
        
        assertTrue(exception.getMessage().contains("Lot code is mandatory for traceability compliance"));
    }
    
    @Test
    void testCreatePackagingRecord_NullLotCode() {
        // Given
        testPackagingRecord.setLotCode(null);
        
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> packagingRecordService.createPackagingRecord(testPackagingRecord)
        );
        
        assertTrue(exception.getMessage().contains("Lot code is mandatory for traceability compliance"));
    }
    
    @Test
    void testValidateLotCode_ValidUniqueCode() {
        // Given
        when(packagingRecordRepository.existsByLotCode("LOT-2026-003")).thenReturn(false);
        
        // When
        boolean result = packagingRecordService.validateLotCode("LOT-2026-003");
        
        // Then
        assertTrue(result);
        verify(packagingRecordRepository).existsByLotCode("LOT-2026-003");
    }
    
    @Test
    void testValidateLotCode_InvalidEmptyCode() {
        // When
        boolean result = packagingRecordService.validateLotCode("");
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testMarkAsExportReady() {
        // Given
        when(packagingRecordRepository.findById("PR-001")).thenReturn(java.util.Optional.of(testPackagingRecord));
        when(packagingRecordRepository.save(any(PackagingRecord.class))).thenReturn(testPackagingRecord);
        
        // When
        PackagingRecord result = packagingRecordService.markAsExportReady("PR-001");
        
        // Then
        assertTrue(result.getExportReady());
        verify(packagingRecordRepository).save(testPackagingRecord);
    }
}