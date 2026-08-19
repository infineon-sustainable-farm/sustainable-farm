package com.sustainablefarm.service;

import com.sustainablefarm.model.Equipment;
import com.sustainablefarm.model.Equipment.EquipmentType;
import com.sustainablefarm.model.Equipment.MaintenanceStatus;
import com.sustainablefarm.repository.EquipmentRepository;
import com.sustainablefarm.service.impl.EquipmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EquipmentService
 * 
 * Tests for:
 * - Inactive Equipment rejection
 * - Equipment availability validation
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class EquipmentServiceTest {

    @Mock
    private EquipmentRepository equipmentRepository;
    
    @InjectMocks
    private EquipmentServiceImpl equipmentService;
    
    private Equipment testEquipment;
    
    @BeforeEach
    void setUp() {
        testEquipment = new Equipment();
        testEquipment.setEquipmentId("EQ-001");
        testEquipment.setEquipmentName("Washing Machine 1");
        testEquipment.setEquipmentType(EquipmentType.WASHING);
        testEquipment.setCapacityKgPerHour(new java.math.BigDecimal("500.0"));
        testEquipment.setEnergyConsumptionKwhPerKg(new java.math.BigDecimal("0.5"));
        testEquipment.setLocation("Production Line A");
        testEquipment.setMaintenanceStatus(MaintenanceStatus.ACTIVE);
    }
    
    @Test
    void testIsEquipmentAvailable_Active() {
        // Given
        when(equipmentRepository.isEquipmentAvailable("EQ-001")).thenReturn(true);
        
        // When
        boolean result = equipmentService.isEquipmentAvailable("EQ-001");
        
        // Then
        assertTrue(result);
        verify(equipmentRepository).isEquipmentAvailable("EQ-001");
    }
    
    @Test
    void testIsEquipmentAvailable_Inactive() {
        // Given
        when(equipmentRepository.isEquipmentAvailable("EQ-001")).thenReturn(false);
        
        // When
        boolean result = equipmentService.isEquipmentAvailable("EQ-001");
        
        // Then
        assertFalse(result);
        verify(equipmentRepository).isEquipmentAvailable("EQ-001");
    }
    
    @Test
    void testCreateEquipmentWithDefaultStatus() {
        // Given
        Equipment newEquipment = new Equipment();
        newEquipment.setEquipmentId("EQ-002");
        when(equipmentRepository.save(any(Equipment.class))).thenReturn(newEquipment);
        
        // When
        Equipment result = equipmentService.createEquipment(newEquipment);
        
        // Then
        assertEquals(MaintenanceStatus.ACTIVE, result.getMaintenanceStatus());
        verify(equipmentRepository).save(newEquipment);
    }
    
    @Test
    void testGetAvailableEquipmentByType() {
        // Given
        when(equipmentRepository.findAvailableByType(EquipmentType.WASHING)).thenReturn(java.util.List.of(testEquipment));
        
        // When
        var result = equipmentService.getAvailableEquipmentByType(EquipmentType.WASHING);
        
        // Then
        assertEquals(1, result.size());
        assertEquals(EquipmentType.WASHING, result.get(0).getEquipmentType());
        verify(equipmentRepository).findAvailableByType(EquipmentType.WASHING);
    }
}