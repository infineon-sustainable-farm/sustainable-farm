package com.sustainablefarm.service;

import com.sustainablefarm.model.DryingRun;
import com.sustainablefarm.model.Equipment;
import com.sustainablefarm.model.Equipment.MaintenanceStatus;
import com.sustainablefarm.model.Equipment.EquipmentType;
import com.sustainablefarm.model.Operator;
import com.sustainablefarm.model.Operator.ActiveStatus;
import com.sustainablefarm.model.Operator.Role;
import com.sustainablefarm.repository.DryingRunRepository;
import com.sustainablefarm.repository.EquipmentRepository;
import com.sustainablefarm.repository.OperatorRepository;
import com.sustainablefarm.service.impl.DryingRunServiceImpl;
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
 * Unit tests for DryingRunService
 * 
 * Tests for:
 * - DryingRun equipment validation
 * - Invalid moisture content
 * - Inactive equipment rejection
 * - Unauthorized operator rejection
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class DryingRunServiceTest {

    @Mock
    private DryingRunRepository dryingRunRepository;
    
    @Mock
    private EquipmentRepository equipmentRepository;
    
    @Mock
    private OperatorRepository operatorRepository;
    
    @InjectMocks
    private DryingRunServiceImpl dryingRunService;
    
    private DryingRun testDryingRun;
    private Equipment testEquipment;
    private Operator testOperator;
    
    @BeforeEach
    void setUp() {
        testDryingRun = new DryingRun();
        testDryingRun.setRunId("DR-001");
        testDryingRun.setDurationHours(24.0);
        testDryingRun.setTargetTemperatureC(60.0);
        testDryingRun.setActualTemperatureC(58.0);
        testDryingRun.setStartMoisturePct(80.0);
        testDryingRun.setEndMoisturePct(15.0);
        testDryingRun.setEnergyUsageKwh(100.0);
        testDryingRun.setStartTime(LocalDateTime.now());
        
        testEquipment = new Equipment();
        testEquipment.setEquipmentId("EQ-001");
        testEquipment.setEquipmentType(EquipmentType.DRYING);
        testEquipment.setMaintenanceStatus(MaintenanceStatus.ACTIVE);
        
        testOperator = new Operator();
        testOperator.setOperatorId("OP-001");
        testOperator.setRole(Role.DRYER);
        testOperator.setActiveStatus(ActiveStatus.ACTIVE);
    }
    
    @Test
    void testCreateDryingRun_WithInactiveEquipment() {
        // Given
        testDryingRun.setEquipment(testEquipment);
        Equipment inactiveEquipment = new Equipment();
        inactiveEquipment.setEquipmentId("EQ-001");
        inactiveEquipment.setEquipmentType(EquipmentType.DRYING);
        inactiveEquipment.setMaintenanceStatus(MaintenanceStatus.UNDER_MAINTENANCE);
        
        when(equipmentRepository.findById("EQ-001")).thenReturn(java.util.Optional.of(inactiveEquipment));
        
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> dryingRunService.createDryingRun(testDryingRun)
        );
        
        assertTrue(exception.getMessage().contains("Equipment is not available for drying"));
        assertTrue(exception.getMessage().contains("must be in ACTIVE status"));
    }
    
    @Test
    void testCreateDryingRun_WithInactiveOperator() {
        // Given
        testDryingRun.setOperator(testOperator);
        Operator inactiveOperator = new Operator();
        inactiveOperator.setOperatorId("OP-001");
        inactiveOperator.setRole(Role.DRYER);
        inactiveOperator.setActiveStatus(ActiveStatus.INACTIVE);
        
        when(operatorRepository.findById("OP-001")).thenReturn(java.util.Optional.of(inactiveOperator));
        
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> dryingRunService.createDryingRun(testDryingRun)
        );
        
        assertTrue(exception.getMessage().contains("Operator is not active for drying"));
        assertTrue(exception.getMessage().contains("must be in ACTIVE status"));
    }
    
    @Test
    void testCreateDryingRun_WithUnauthorizedOperator() {
        // Given
        testDryingRun.setOperator(testOperator);
        Operator unauthorizedOperator = new Operator();
        unauthorizedOperator.setOperatorId("OP-001");
        unauthorizedOperator.setRole(Role.WASHER); // Wrong role
        unauthorizedOperator.setActiveStatus(ActiveStatus.ACTIVE);
        
        when(operatorRepository.findById("OP-001")).thenReturn(java.util.Optional.of(unauthorizedOperator));
        
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> dryingRunService.createDryingRun(testDryingRun)
        );
        
        assertTrue(exception.getMessage().contains("Operator must have DRYER role"));
    }
    
    @Test
    void testCompleteDryingRun_InvalidMoistureContent() {
        // Given
        when(dryingRunRepository.findById("DR-001")).thenReturn(java.util.Optional.of(testDryingRun));
        when(dryingRunRepository.save(any(DryingRun.class))).thenReturn(testDryingRun);
        
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> dryingRunService.completeDryingRun("DR-001", 19.0)
        );
        
        assertTrue(exception.getMessage().contains("End moisture content must be between 6% and 18%"));
    }
    
    @Test
    void testCompleteDryingRun_ValidMoistureContent() {
        // Given
        when(dryingRunRepository.findById("DR-001")).thenReturn(java.util.Optional.of(testDryingRun));
        when(dryingRunRepository.save(any(DryingRun.class))).thenReturn(testDryingRun);
        
        // When
        DryingRun result = dryingRunService.completeDryingRun("DR-001", 14.0);
        
        // Then
        assertEquals(14.0, result.getEndMoisturePct());
        verify(dryingRunRepository).save(testDryingRun);
    }
    
    @Test
    void testCreateDryingRun_WithValidEquipmentAndOperator() {
        // Given
        testDryingRun.setEquipment(testEquipment);
        testDryingRun.setOperator(testOperator);
        when(equipmentRepository.findById("EQ-001")).thenReturn(java.util.Optional.of(testEquipment));
        when(operatorRepository.findById("OP-001")).thenReturn(java.util.Optional.of(testOperator));
        when(dryingRunRepository.save(any(DryingRun.class))).thenReturn(testDryingRun);
        
        // When
        DryingRun result = dryingRunService.createDryingRun(testDryingRun);
        
        // Then
        assertEquals(testDryingRun, result);
        verify(dryingRunRepository).save(testDryingRun);
    }
}