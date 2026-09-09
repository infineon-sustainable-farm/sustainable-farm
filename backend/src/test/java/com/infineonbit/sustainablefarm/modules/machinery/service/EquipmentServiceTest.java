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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EquipmentServiceTest {
    @Mock
    private EquipmentRepository equipmentRepository;

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
        when(equipmentRepository.findById(equipment.getId())).thenReturn(Optional.of(equipment));
        // Act
         equipmentService.deleteEquipment(equipment.getId());
        //
        verify(equipmentRepository).deleteById(equipment.getId());
    }

    @Test
    void deleteEquipment_shouldNotDelete_whenIdNotExist(){
        // Arrange
        Long nonExistendId = 96L;
        when(equipmentRepository.findById(nonExistendId)).thenReturn(Optional.empty());
        // Act
        EquipmentNotFoundException ex = assertThrows(EquipmentNotFoundException.class, ()->{equipmentService.deleteEquipment(nonExistendId);} );
        // Assert
        // Assert
        assertEquals("Equipment with ID 96 not found", ex.getMessage());
    }

}
