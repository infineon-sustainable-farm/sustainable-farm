package com.sustainablefarm.service;

import com.sustainablefarm.model.WashSortRecord;
import com.sustainablefarm.model.Equipment;
import com.sustainablefarm.model.Equipment.MaintenanceStatus;
import com.sustainablefarm.model.Equipment.EquipmentType;
import com.sustainablefarm.model.Operator;
import com.sustainablefarm.model.Operator.ActiveStatus;
import com.sustainablefarm.model.Operator.Role;
import com.sustainablefarm.repository.WashSortRecordRepository;
import com.sustainablefarm.repository.EquipmentRepository;
import com.sustainablefarm.repository.OperatorRepository;
import com.sustainablefarm.service.impl.WashSortRecordServiceImpl;
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
 * Unit tests for WashSortRecordService
 * 
 * Tests for:
 * - WashSort equipment validation
 * - Inactive equipment rejection
 * - Unauthorized operator rejection
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class WashSortRecordServiceTest {

    @Mock
    private WashSortRecordRepository washSortRecordRepository;
    
    @Mock
    private EquipmentRepository equipmentRepository;
    
    @Mock
    private OperatorRepository operatorRepository;
    
    @InjectMocks
    private WashSortRecordServiceImpl washSortRecordService;
    
    private WashSortRecord testWashSortRecord;
    private Equipment testEquipment;
    private Operator testOperator;
    
    @BeforeEach
    void setUp() {
        testWashSortRecord = new WashSortRecord();
        testWashSortRecord.setRecordId("WS-001");
        testWashSortRecord.setInputQuantityKg(1000.0);
        testWashSortRecord.setOutputQuantityKg(950.0);
        testWashSortRecord.setWasteQuantityKg(50.0);
        testWashSortRecord.setWaterUsageLiters(200.0);
        testWashSortRecord.setStartTime(LocalDateTime.now());
        
        testEquipment = new Equipment();
        testEquipment.setEquipmentId("EQ-001");
        testEquipment.setEquipmentType(EquipmentType.WASHING);
        testEquipment.setMaintenanceStatus(MaintenanceStatus.ACTIVE);
        
        testOperator = new Operator();
        testOperator.setOperatorId("OP-001");
        testOperator.setRole(Role.WASHER);
        testOperator.setActiveStatus(ActiveStatus.ACTIVE);
    }
    
    @Test
    void testCreateWashSortRecord_WithInactiveEquipment() {
        // Given
        testWashSortRecord.setEquipment(testEquipment);
        Equipment inactiveEquipment = new Equipment();
        inactiveEquipment.setEquipmentId("EQ-001");
        inactiveEquipment.setEquipmentType(EquipmentType.WASHING);
        inactiveEquipment.setMaintenanceStatus(MaintenanceStatus.UNDER_MAINTENANCE);
        
        when(equipmentRepository.findById("EQ-001")).thenReturn(java.util.Optional.of(inactiveEquipment));
        
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> washSortRecordService.createWashSortRecord(testWashSortRecord)
        );
        
        assertTrue(exception.getMessage().contains("Equipment is not available for washing"));
        assertTrue(exception.getMessage().contains("must be in ACTIVE status"));
    }
    
    @Test
    void testCreateWashSortRecord_WithInactiveOperator() {
        // Given
        testWashSortRecord.setOperator(testOperator);
        Operator inactiveOperator = new Operator();
        inactiveOperator.setOperatorId("OP-001");
        inactiveOperator.setRole(Role.WASHER);
        inactiveOperator.setActiveStatus(ActiveStatus.INACTIVE);
        
        when(operatorRepository.findById("OP-001")).thenReturn(java.util.Optional.of(inactiveOperator));
        
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> washSortRecordService.createWashSortRecord(testWashSortRecord)
        );
        
        assertTrue(exception.getMessage().contains("Operator is not active for washing"));
        assertTrue(exception.getMessage().contains("must be in ACTIVE status"));
    }
    
    @Test
    void testCreateWashSortRecord_WithUnauthorizedOperator() {
        // Given
        testWashSortRecord.setOperator(testOperator);
        Operator unauthorizedOperator = new Operator();
        unauthorizedOperator.setOperatorId("OP-001");
        unauthorizedOperator.setRole(Role.DRYER); // Wrong role
        unauthorizedOperator.setActiveStatus(ActiveStatus.ACTIVE);
        
        when(operatorRepository.findById("OP-001")).thenReturn(java.util.Optional.of(unauthorizedOperator));
        
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> washSortRecordService.createWashSortRecord(testWashSortRecord)
        );
        
        assertTrue(exception.getMessage().contains("Operator must have WASHER role"));
    }
    
    @Test
    void testCreateWashSortRecord_WithValidEquipmentAndOperator() {
        // Given
        testWashSortRecord.setEquipment(testEquipment);
        testWashSortRecord.setOperator(testOperator);
        when(equipmentRepository.findById("EQ-001")).thenReturn(java.util.Optional.of(testEquipment));
        when(operatorRepository.findById("OP-001")).thenReturn(java.util.Optional.of(testOperator));
        when(washSortRecordRepository.save(any(WashSortRecord.class))).thenReturn(testWashSortRecord);
        
        // When
        WashSortRecord result = washSortRecordService.createWashSortRecord(testWashSortRecord);
        
        // Then
        assertEquals(testWashSortRecord, result);
        verify(washSortRecordRepository).save(testWashSortRecord);
    }
    
    @Test
    void testGetAverageYieldPercentageByBatch() {
        // Given
        when(washSortRecordRepository.getAverageYieldPercentageByBatch("B-001")).thenReturn(95.0);
        
        // When
        Double result = washSortRecordService.getAverageYieldPercentageByBatch("B-001");
        
        // Then
        assertEquals(95.0, result);
        verify(washSortRecordRepository).getAverageYieldPercentageByBatch("B-001");
    }
    
    @Test
    void testCompleteWashSortRecord() {
        // Given
        when(washSortRecordRepository.findById("WS-001")).thenReturn(java.util.Optional.of(testWashSortRecord));
        when(washSortRecordRepository.save(any(WashSortRecord.class))).thenReturn(testWashSortRecord);
        
        // When
        WashSortRecord result = washSortRecordService.completeWashSortRecord("WS-001", 950.0, 50.0);
        
        // Then
        assertEquals(950.0, result.getOutputQuantityKg());
        assertEquals(50.0, result.getWasteQuantityKg());
        assertNotNull(result.getEndTime());
        verify(washSortRecordRepository).save(testWashSortRecord);
    }
}