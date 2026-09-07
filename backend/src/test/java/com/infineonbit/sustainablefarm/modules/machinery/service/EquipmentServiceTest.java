package com.infineonbit.sustainablefarm.modules.machinery.service;

import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.EquipmentCreationRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Response.EquipmentObtainingResponse;
import com.infineonbit.sustainablefarm.modules.machinery.enums.Category;
import com.infineonbit.sustainablefarm.modules.machinery.enums.Stage;
import com.infineonbit.sustainablefarm.modules.machinery.enums.Status;
import com.infineonbit.sustainablefarm.modules.machinery.repository.EquipmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void addEquipment_shouldCreateEquipment_whenNameExist(){
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
}
