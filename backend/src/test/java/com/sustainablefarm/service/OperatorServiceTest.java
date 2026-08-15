package com.sustainablefarm.service;

import com.sustainablefarm.model.Operator;
import com.sustainablefarm.model.Operator.ActiveStatus;
import com.sustainablefarm.model.Operator.Role;
import com.sustainablefarm.repository.OperatorRepository;
import com.sustainablefarm.service.impl.OperatorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OperatorService
 * 
 * Tests for:
 * - Unauthorized Operator rejection
 * - Operator role validation
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class OperatorServiceTest {

    @Mock
    private OperatorRepository operatorRepository;
    
    @InjectMocks
    private OperatorServiceImpl operatorService;
    
    private Operator testOperator;
    
    @BeforeEach
    void setUp() {
        testOperator = new Operator();
        testOperator.setOperatorId("OP-001");
        testOperator.setOperatorName("John Doe");
        testOperator.setRole(Role.WASHER);
        testOperator.setActiveStatus(ActiveStatus.ACTIVE);
        testOperator.setHireDate(java.time.LocalDate.of(2023, 1, 1));
    }
    
    @Test
    void testIsOperatorActive_Active() {
        // Given
        when(operatorRepository.isOperatorActive("OP-001")).thenReturn(true);
        
        // When
        boolean result = operatorService.isOperatorActive("OP-001");
        
        // Then
        assertTrue(result);
        verify(operatorRepository).isOperatorActive("OP-001");
    }
    
    @Test
    void testIsOperatorActive_Inactive() {
        // Given
        when(operatorRepository.isOperatorActive("OP-001")).thenReturn(false);
        
        // When
        boolean result = operatorService.isOperatorActive("OP-001");
        
        // Then
        assertFalse(result);
        verify(operatorRepository).isOperatorActive("OP-001");
    }
    
    @Test
    void testHasRole_CorrectRole() {
        // Given
        when(operatorRepository.hasRole("OP-001", Role.WASHER)).thenReturn(true);
        
        // When
        boolean result = operatorService.hasRole("OP-001", Role.WASHER);
        
        // Then
        assertTrue(result);
        verify(operatorRepository).hasRole("OP-001", Role.WASHER);
    }
    
    @Test
    void testHasRole_WrongRole() {
        // Given
        when(operatorRepository.hasRole("OP-001", Role.QC_INSPECTOR)).thenReturn(false);
        
        // When
        boolean result = operatorService.hasRole("OP-001", Role.QC_INSPECTOR);
        
        // Then
        assertFalse(result);
        verify(operatorRepository).hasRole("OP-001", Role.QC_INSPECTOR);
    }
    
    @Test
    void testGetActiveOperatorsByRole() {
        // Given
        when(operatorRepository.findActiveByRole(Role.WASHER)).thenReturn(java.util.List.of(testOperator));
        
        // When
        var result = operatorService.getActiveOperatorsByRole(Role.WASHER);
        
        // Then
        assertEquals(1, result.size());
        assertEquals(Role.WASHER, result.get(0).getRole());
        assertEquals(ActiveStatus.ACTIVE, result.get(0).getActiveStatus());
        verify(operatorRepository).findActiveByRole(Role.WASHER);
    }
}