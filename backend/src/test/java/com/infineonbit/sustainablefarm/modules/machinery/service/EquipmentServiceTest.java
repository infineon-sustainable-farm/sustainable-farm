package com.infineonbit.sustainablefarm.modules.machinery.service;

import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.EquipmentCreationRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.EquipmentStatusUpdateRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Response.EquipmentObtainingResponse;
import com.infineonbit.sustainablefarm.modules.machinery.entity.Equipment;
import com.infineonbit.sustainablefarm.modules.machinery.enums.Category;
import com.infineonbit.sustainablefarm.modules.machinery.enums.Stage;
import com.infineonbit.sustainablefarm.modules.machinery.enums.Status;
import com.infineonbit.sustainablefarm.modules.machinery.exception.EquipmentNotFoundException;
import com.infineonbit.sustainablefarm.modules.machinery.repository.EquipmentRepository;
import com.infineonbit.sustainablefarm.modules.machinery.repository.SparePartRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EquipmentServiceTest {
    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private SparePartRepository sparePartRepository;

    @InjectMocks
    private EquipmentService equipmentService;

    @Test
    void addEquipment_shouldCreateEquipment_whenNameDoesNotExist(){
        // Arrange
        EquipmentCreationRequest equipmentCreationRequest = new EquipmentCreationRequest(
                "Tractor",
                Category.AGRICULTURAL_MACHINERY,
                Stage.CULTIVATION,
                Status.OPERATIONAL
        );
        when(equipmentRepository.existsByName("Tractor")).thenReturn(false);
        // Act
        EquipmentObtainingResponse equipmentObtainingResponse = equipmentService.addEquipment(equipmentCreationRequest);
        // Assert
        assertEquals("Tractor",equipmentObtainingResponse.name());
    }

    @Test
    void addEquipment_shouldThrowException_whenNameAlreadyExists(){
        // Arrange
        EquipmentCreationRequest equipmentCreationRequest = new EquipmentCreationRequest(
                "Tractor",
                Category.AGRICULTURAL_MACHINERY,
                Stage.CULTIVATION,
                Status.OPERATIONAL
        );
        when(equipmentRepository.existsByName("Tractor")).thenReturn(true);
        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            equipmentService.addEquipment(equipmentCreationRequest);
        });
        // Assert
        assertEquals("Tractor already exists.", ex.getMessage());
    }

    @Test
    void updateEquipmentStatus_shouldUpdateEquipment_whenIdExists(){
        //Arrange
        Equipment equipment = new Equipment(
                6L,
                "Tractor",
                Category.AGRICULTURAL_MACHINERY,
                Stage.CULTIVATION,
                Status.OUT_OF_SERVICE
        );

        Optional<Equipment> jpaEquipment = Optional.of(equipment);
        EquipmentStatusUpdateRequest equipmentStatusUpdateRequest = new EquipmentStatusUpdateRequest(Status.OUT_OF_SERVICE);
        when(equipmentRepository.updateEquipmentStatusById(equipment.getId(), equipment.getStatus())).thenReturn(6);
        when(equipmentRepository.findById(equipment.getId())).thenReturn(jpaEquipment);
        // Act
        EquipmentObtainingResponse equipmentObtainingResponse = equipmentService.updateEquipmentStatus(6L, equipmentStatusUpdateRequest);
        // Assert
        assertEquals(Status.OUT_OF_SERVICE,equipmentObtainingResponse.status());
    }

    @Test
    void updateEquipmentStatus_shouldThrowException_whenIdDoesNotExist(){
        // Arrange
        Long nonExistentId = 99L;
        EquipmentStatusUpdateRequest equipmentStatusUpdateRequest = new EquipmentStatusUpdateRequest(Status.OUT_OF_SERVICE);
        when(equipmentRepository.updateEquipmentStatusById(nonExistentId, equipmentStatusUpdateRequest.status())).thenReturn(0);

        // Act
        EquipmentNotFoundException ex = assertThrows(EquipmentNotFoundException.class, () -> {
            equipmentService.updateEquipmentStatus(nonExistentId, equipmentStatusUpdateRequest);
        });

        // Assert
        assertEquals("Equipment with ID 99 not found", ex.getMessage());
    }

    @Test
    void deleteEquipment_shouldDelete_whenIdExist(){
        // Arrange
        Equipment equipment = new Equipment(
                6L,
                "Tractor",
                Category.AGRICULTURAL_MACHINERY,
                Stage.CULTIVATION,
                Status.OUT_OF_SERVICE
        );
        when(equipmentRepository.existsById(equipment.getId())).thenReturn(true);
        when(equipmentRepository.deleteEquipmentById(equipment.getId())).thenReturn(1);
        // Act
         equipmentService.deleteEquipment(equipment.getId());
        //
        verify(equipmentRepository).deleteEquipmentById(equipment.getId());
    }

    @Test
    void deleteEquipment_shouldDetachSparePartsBeforeDeleting() {
        // Arrange
        when(equipmentRepository.existsById(6L)).thenReturn(true);
        when(equipmentRepository.deleteEquipmentById(6L)).thenReturn(1);

        // Act
        equipmentService.deleteEquipment(6L);

        // Assert: spare parts are detached first, then the equipment is removed
        InOrder deletionOrder = inOrder(sparePartRepository, equipmentRepository);
        deletionOrder.verify(sparePartRepository).detachAllFromEquipment(6L);
        deletionOrder.verify(equipmentRepository).deleteEquipmentById(6L);
    }

    @Test
    void deleteEquipment_shouldNotDelete_whenIdNotExist(){
        // Arrange
        Long nonExistendId = 96L;
        when(equipmentRepository.existsById(nonExistendId)).thenReturn(false);
        // Act
        EquipmentNotFoundException ex = assertThrows(EquipmentNotFoundException.class, ()->{equipmentService.deleteEquipment(nonExistendId);} );
        // Assert
        assertEquals("Equipment with ID 96 not found", ex.getMessage());
        verify(sparePartRepository, never()).detachAllFromEquipment(any());
        verify(equipmentRepository, never()).deleteEquipmentById(any());
    }

    @Test
    void deleteEquipment_shouldReportNotFound_whenEquipmentDeletedConcurrently(){
        // Arrange: another transaction deletes the row between the existence check and the delete
        when(equipmentRepository.existsById(7L)).thenReturn(true);
        when(equipmentRepository.deleteEquipmentById(7L)).thenReturn(0);

        // Act
        EquipmentNotFoundException ex = assertThrows(EquipmentNotFoundException.class,
                ()->{equipmentService.deleteEquipment(7L);});

        // Assert
        assertEquals("Equipment with ID 7 not found", ex.getMessage());
    }

    @Test
    void addEquipment_shouldThrowClearError_whenDuplicateInsertRaces(){
        // Arrange: both threads pass the existsByName check, the loser hits the unique constraint
        EquipmentCreationRequest request = new EquipmentCreationRequest(
                "Tractor",
                Category.AGRICULTURAL_MACHINERY,
                Stage.CULTIVATION,
                Status.OPERATIONAL
        );
        when(equipmentRepository.existsByName("Tractor"))
                .thenReturn(false)
                .thenReturn(true);
        doThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"))
                .when(equipmentRepository).save(any(Equipment.class));

        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> equipmentService.addEquipment(request));

        // Assert
        assertEquals("Tractor already exists.", ex.getMessage());
    }

}
