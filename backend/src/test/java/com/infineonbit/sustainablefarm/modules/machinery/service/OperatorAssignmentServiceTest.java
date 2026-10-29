package com.infineonbit.sustainablefarm.modules.machinery.service;

import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.OperatorAssignmentCreationRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.OperatorAssignmentUpdateRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Response.OperatorAssignmentObtainingResponse;
import com.infineonbit.sustainablefarm.modules.machinery.entity.Equipment;
import com.infineonbit.sustainablefarm.modules.machinery.entity.OperatorAssignment;
import com.infineonbit.sustainablefarm.modules.machinery.enums.Category;
import com.infineonbit.sustainablefarm.modules.machinery.enums.Stage;
import com.infineonbit.sustainablefarm.modules.machinery.enums.Status;
import com.infineonbit.sustainablefarm.modules.machinery.exception.EquipmentAlreadyAssignedException;
import com.infineonbit.sustainablefarm.modules.machinery.exception.EquipmentNotFoundException;
import com.infineonbit.sustainablefarm.modules.machinery.exception.OperatorAssignmentNotFoundException;
import com.infineonbit.sustainablefarm.modules.machinery.repository.EquipmentRepository;
import com.infineonbit.sustainablefarm.modules.machinery.repository.OperatorAssignmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OperatorAssignmentServiceTest {

    private static final LocalDate START_DATE = LocalDate.of(2026, 1, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 12, 31);

    @Mock
    private OperatorAssignmentRepository operatorAssignmentRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @InjectMocks
    private OperatorAssignmentService operatorAssignmentService;

    private Equipment buildEquipment(Long id) {
        return new Equipment(id, "Tractor", Category.AGRICULTURAL_MACHINERY,
                Stage.CULTIVATION, Status.OPERATIONAL);
    }

    @Test
    void addOperatorAssignment_shouldCreateAssignment_whenEquipmentExistsAndIsFree() {
        // Arrange
        Equipment equipment = buildEquipment(6L);
        OperatorAssignmentCreationRequest request = new OperatorAssignmentCreationRequest(
                " John Doe ", 6L, " Operator ", START_DATE, END_DATE
        );
        when(equipmentRepository.findById(6L)).thenReturn(Optional.of(equipment));
        when(operatorAssignmentRepository.existsByEquipment_Id(6L)).thenReturn(false);
        // Act
        OperatorAssignmentObtainingResponse response = operatorAssignmentService.addOperatorAssignment(request);
        // Assert
        assertEquals("John Doe", response.fullName());
        assertEquals(6L, response.equipmentId());
        assertEquals("Operator", response.jobTitle());
        assertEquals(START_DATE, response.startDate());
        assertEquals(END_DATE, response.endDate());
        verify(operatorAssignmentRepository).save(any(OperatorAssignment.class));
    }

    @Test
    void addOperatorAssignment_shouldCreateAssignment_whenNoEndDateProvided() {
        // Arrange
        Equipment equipment = buildEquipment(6L);
        OperatorAssignmentCreationRequest request = new OperatorAssignmentCreationRequest(
                "John Doe", 6L, "Operator", START_DATE, null
        );
        when(equipmentRepository.findById(6L)).thenReturn(Optional.of(equipment));
        when(operatorAssignmentRepository.existsByEquipment_Id(6L)).thenReturn(false);
        // Act
        OperatorAssignmentObtainingResponse response = operatorAssignmentService.addOperatorAssignment(request);
        // Assert
        assertEquals("John Doe", response.fullName());
        assertEquals(6L, response.equipmentId());
        assertEquals(START_DATE, response.startDate());
        assertNull(response.endDate());
        verify(operatorAssignmentRepository).save(any(OperatorAssignment.class));
    }

    @Test
    void addOperatorAssignment_shouldThrowException_whenEquipmentDoesNotExist() {
        // Arrange
        Long nonExistentEquipmentId = 99L;
        OperatorAssignmentCreationRequest request = new OperatorAssignmentCreationRequest(
                "John Doe", nonExistentEquipmentId, "Operator", START_DATE, END_DATE
        );
        when(equipmentRepository.findById(nonExistentEquipmentId)).thenReturn(Optional.empty());
        // Act
        EquipmentNotFoundException ex = assertThrows(EquipmentNotFoundException.class, () -> {
            operatorAssignmentService.addOperatorAssignment(request);
        });
        // Assert
        assertEquals("Equipment with ID 99 not found", ex.getMessage());
    }

    @Test
    void addOperatorAssignment_shouldThrowException_whenEquipmentAlreadyAssigned() {
        // Arrange
        Equipment equipment = buildEquipment(6L);
        OperatorAssignmentCreationRequest request = new OperatorAssignmentCreationRequest(
                "John Doe", 6L, "Operator", START_DATE, END_DATE
        );
        when(equipmentRepository.findById(6L)).thenReturn(Optional.of(equipment));
        when(operatorAssignmentRepository.existsByEquipment_Id(6L)).thenReturn(true);
        // Act
        EquipmentAlreadyAssignedException ex = assertThrows(EquipmentAlreadyAssignedException.class, () -> {
            operatorAssignmentService.addOperatorAssignment(request);
        });
        // Assert
        assertEquals("Equipment with ID 6 is already assigned to an operator.", ex.getMessage());
    }

    @Test
    void addOperatorAssignment_shouldThrowException_whenEndDateIsBeforeStartDate() {
        // Arrange
        Equipment equipment = buildEquipment(6L);
        LocalDate beforeStart = START_DATE.minusDays(1);
        OperatorAssignmentCreationRequest request = new OperatorAssignmentCreationRequest(
                "John Doe", 6L, "Operator", START_DATE, beforeStart
        );
        when(equipmentRepository.findById(6L)).thenReturn(Optional.of(equipment));
        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            operatorAssignmentService.addOperatorAssignment(request);
        });
        // Assert
        assertEquals("End date cannot be before start date.", ex.getMessage());
    }

    @Test
    void addOperatorAssignment_shouldThrowAlreadyAssigned_whenUniqueConstraintViolated() {
        // Arrange
        Equipment equipment = buildEquipment(6L);
        OperatorAssignmentCreationRequest request = new OperatorAssignmentCreationRequest(
                "John Doe", 6L, "Operator", START_DATE, END_DATE
        );
        when(equipmentRepository.findById(6L)).thenReturn(Optional.of(equipment));
        when(operatorAssignmentRepository.existsByEquipment_Id(6L))
                .thenReturn(false, true);
        when(operatorAssignmentRepository.save(any(OperatorAssignment.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("unique violation"));
        // Act
        EquipmentAlreadyAssignedException ex = assertThrows(EquipmentAlreadyAssignedException.class, () -> {
            operatorAssignmentService.addOperatorAssignment(request);
        });
        // Assert
        assertEquals("Equipment with ID 6 is already assigned to an operator.", ex.getMessage());
    }

    @Test
    void obtainAllOperatorAssignments_shouldReturnAllAssignments_whenSomeExist() {
        // Arrange
        Equipment equipment = buildEquipment(6L);
        OperatorAssignment assignment = new OperatorAssignment(
                1L, "John Doe", equipment, "Operator", START_DATE, END_DATE, null
        );
        Pageable pageable = PageRequest.of(0, 10);
        when(operatorAssignmentRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(assignment)));
        // Act
        Page<OperatorAssignmentObtainingResponse> responses = operatorAssignmentService
                .obtainAllOperatorAssignments(pageable);
        // Assert
        assertEquals(1, responses.getTotalElements());
        OperatorAssignmentObtainingResponse response = responses.getContent().get(0);
        assertEquals(1L, response.id());
        assertEquals("John Doe", response.fullName());
        assertEquals(6L, response.equipmentId());
        assertEquals("Operator", response.jobTitle());
        assertEquals(START_DATE, response.startDate());
        assertEquals(END_DATE, response.endDate());
    }

    @Test
    void obtainAllOperatorAssignments_shouldReturnEmptyPage_whenNoneExist() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(operatorAssignmentRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());
        // Act
        Page<OperatorAssignmentObtainingResponse> responses = operatorAssignmentService
                .obtainAllOperatorAssignments(pageable);
        // Assert
        assertTrue(responses.isEmpty());
    }

    @Test
    void updateOperatorAssignment_shouldUpdateAllFields_whenOnlyExistingAssignment() {
        // Arrange
        Equipment oldEquipment = buildEquipment(6L);
        Equipment newEquipment = buildEquipment(7L);
        OperatorAssignment assignment = new OperatorAssignment(
                1L, "John Doe", oldEquipment, "Operator", START_DATE, END_DATE, null
        );
        LocalDate newStart = LocalDate.of(2027, 2, 1);
        LocalDate newEnd = LocalDate.of(2027, 11, 30);
        OperatorAssignmentUpdateRequest request = new OperatorAssignmentUpdateRequest(
                " Jane Smith ", 7L, " Supervisor ", newStart, newEnd
        );
        when(operatorAssignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));
        when(equipmentRepository.findById(7L)).thenReturn(Optional.of(newEquipment));
        when(operatorAssignmentRepository.existsByEquipment_Id(7L)).thenReturn(false);
        // Act
        OperatorAssignmentObtainingResponse response = operatorAssignmentService.updateOperatorAssignment(1L, request);
        // Assert
        assertEquals("Jane Smith", response.fullName());
        assertEquals(7L, response.equipmentId());
        assertEquals("Supervisor", response.jobTitle());
        assertEquals(newStart, response.startDate());
        assertEquals(newEnd, response.endDate());
        verify(operatorAssignmentRepository).save(assignment);
    }

    @Test
    void updateOperatorAssignment_shouldOnlyUpdateEndDate_whenOnlyEndDateProvided() {
        // Arrange
        Equipment equipment = buildEquipment(6L);
        OperatorAssignment assignment = new OperatorAssignment(
                1L, "John Doe", equipment, "Operator", START_DATE, null, null
        );
        OperatorAssignmentUpdateRequest request = new OperatorAssignmentUpdateRequest(
                null, null, null, null, END_DATE
        );
        when(operatorAssignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));
        // Act
        OperatorAssignmentObtainingResponse response = operatorAssignmentService.updateOperatorAssignment(1L, request);
        // Assert
        assertEquals(END_DATE, response.endDate());
        assertEquals(START_DATE, response.startDate());
        assertEquals("John Doe", response.fullName());
        assertEquals(6L, response.equipmentId());
    }

    @Test
    void updateOperatorAssignment_shouldNotChangeAnything_whenNoFieldProvided() {
        // Arrange
        Equipment equipment = buildEquipment(6L);
        OperatorAssignment assignment = new OperatorAssignment(
                1L, "John Doe", equipment, "Operator", START_DATE, END_DATE, null
        );
        OperatorAssignmentUpdateRequest request = new OperatorAssignmentUpdateRequest(null, null, null, null, null);
        when(operatorAssignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));
        // Act
        OperatorAssignmentObtainingResponse response = operatorAssignmentService.updateOperatorAssignment(1L, request);
        // Assert
        assertEquals("John Doe", response.fullName());
        assertEquals(6L, response.equipmentId());
        assertEquals("Operator", response.jobTitle());
        assertEquals(START_DATE, response.startDate());
        assertEquals(END_DATE, response.endDate());
    }

    @Test
    void updateOperatorAssignment_shouldThrowNotFound_whenAssignmentDoesNotExist() {
        // Arrange
        when(operatorAssignmentRepository.findById(99L)).thenReturn(Optional.empty());
        // Act
        OperatorAssignmentNotFoundException ex = assertThrows(
                OperatorAssignmentNotFoundException.class,
                () -> operatorAssignmentService.updateOperatorAssignment(99L,
                        new OperatorAssignmentUpdateRequest(null, null, null, null, null)));
        // Assert
        assertEquals("Operator assignment with ID 99 not found", ex.getMessage());
    }

    @Test
    void updateOperatorAssignment_shouldThrowEquipmentNotFound_whenNewEquipmentDoesNotExist() {
        // Arrange
        Equipment equipment = buildEquipment(6L);
        OperatorAssignment assignment = new OperatorAssignment(
                1L, "John Doe", equipment, "Operator", START_DATE, null, null
        );
        when(operatorAssignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));
        when(equipmentRepository.findById(99L)).thenReturn(Optional.empty());
        // Act
        EquipmentNotFoundException ex = assertThrows(EquipmentNotFoundException.class,
                () -> operatorAssignmentService.updateOperatorAssignment(1L,
                        new OperatorAssignmentUpdateRequest(null, 99L, null, null, null)));
        // Assert
        assertEquals("Equipment with ID 99 not found", ex.getMessage());
    }

    @Test
    void updateOperatorAssignment_shouldThrowAlreadyAssigned_whenNewEquipmentAlreadyAssigned() {
        // Arrange
        Equipment oldEquipment = buildEquipment(6L);
        Equipment newEquipment = buildEquipment(7L);
        OperatorAssignment assignment = new OperatorAssignment(
                1L, "John Doe", oldEquipment, "Operator", START_DATE, null, null
        );
        when(operatorAssignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));
        when(equipmentRepository.findById(7L)).thenReturn(Optional.of(newEquipment));
        when(operatorAssignmentRepository.existsByEquipment_Id(7L)).thenReturn(true);
        // Act
        EquipmentAlreadyAssignedException ex = assertThrows(EquipmentAlreadyAssignedException.class,
                () -> operatorAssignmentService.updateOperatorAssignment(1L,
                        new OperatorAssignmentUpdateRequest(null, 7L, null, null, null)));
        // Assert
        assertEquals("Equipment with ID 7 is already assigned to an operator.", ex.getMessage());
    }

    @Test
    void updateOperatorAssignment_shouldThrowException_whenEndDateIsBeforeStoredStartDate() {
        // Arrange
        Equipment equipment = buildEquipment(6L);
        OperatorAssignment assignment = new OperatorAssignment(
                1L, "John Doe", equipment, "Operator", START_DATE, null, null
        );
        OperatorAssignmentUpdateRequest request = new OperatorAssignmentUpdateRequest(
                null, null, null, null, START_DATE.minusDays(1)
        );
        when(operatorAssignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));
        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> operatorAssignmentService.updateOperatorAssignment(1L, request));
        // Assert
        assertEquals("End date cannot be before the stored start date.", ex.getMessage());
    }

    @Test
    void updateOperatorAssignment_shouldThrowException_whenStartDateIsAfterStoredEndDate() {
        // Arrange
        Equipment equipment = buildEquipment(6L);
        OperatorAssignment assignment = new OperatorAssignment(
                1L, "John Doe", equipment, "Operator", START_DATE, END_DATE, null
        );
        OperatorAssignmentUpdateRequest request = new OperatorAssignmentUpdateRequest(
                null, null, null, END_DATE.plusDays(1), null
        );
        when(operatorAssignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));
        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> operatorAssignmentService.updateOperatorAssignment(1L, request));
        // Assert
        assertEquals("Start date cannot be after the stored end date.", ex.getMessage());
    }

    @Test
    void updateOperatorAssignment_shouldThrowException_whenFullNameIsBlank() {
        // Arrange
        Equipment equipment = buildEquipment(6L);
        OperatorAssignment assignment = new OperatorAssignment(
                1L, "John Doe", equipment, "Operator", START_DATE, null, null
        );
        OperatorAssignmentUpdateRequest request = new OperatorAssignmentUpdateRequest(
                "   ", null, null, null, null
        );
        when(operatorAssignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));
        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> operatorAssignmentService.updateOperatorAssignment(1L, request));
        // Assert
        assertEquals("Full name cannot be blank.", ex.getMessage());
    }

    @Test
    void deleteOperatorAssignment_shouldDelete_whenIdExists() {
        // Arrange
        when(operatorAssignmentRepository.existsById(1L)).thenReturn(true);
        // Act
        operatorAssignmentService.deleteOperatorAssignment(1L);
        // Assert
        verify(operatorAssignmentRepository).deleteById(1L);
    }

    @Test
    void deleteOperatorAssignment_shouldThrowException_whenIdDoesNotExist() {
        // Arrange
        when(operatorAssignmentRepository.existsById(99L)).thenReturn(false);
        // Act
        OperatorAssignmentNotFoundException ex = assertThrows(OperatorAssignmentNotFoundException.class,
                () -> operatorAssignmentService.deleteOperatorAssignment(99L));
        // Assert
        assertEquals("Operator assignment with ID 99 not found", ex.getMessage());
        verify(operatorAssignmentRepository, never()).deleteById(anyLong());
    }
}