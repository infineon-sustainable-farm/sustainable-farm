package com.infineonbit.sustainablefarm.modules.machinery.service;

import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.MaintenanceScheduleCreationRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.MaintenanceScheduleUpdateRequest;
import com.infineonbit.sustainablefarm.modules.machinery.entity.Equipment;
import com.infineonbit.sustainablefarm.modules.machinery.entity.MaintenanceSchedule;
import com.infineonbit.sustainablefarm.modules.machinery.enums.MaintenanceType;
import com.infineonbit.sustainablefarm.modules.machinery.exception.EquipmentNotFoundException;
import com.infineonbit.sustainablefarm.modules.machinery.exception.MaintenanceScheduleNotFoundException;
import com.infineonbit.sustainablefarm.modules.machinery.repository.EquipmentRepository;
import com.infineonbit.sustainablefarm.modules.machinery.repository.MaintenanceScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceScheduleServiceTest {

    @Mock
    private MaintenanceScheduleRepository maintenanceScheduleRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @InjectMocks
    private MaintenanceScheduleService maintenanceScheduleService;

    private Equipment testEquipment;
    private MaintenanceSchedule testSchedule;
    private MaintenanceScheduleCreationRequest validRequest;

    @BeforeEach
    void setUp() {
        testEquipment = new Equipment();
        testEquipment.setId(1L);

        validRequest = new MaintenanceScheduleCreationRequest(
                1L,
                MaintenanceType.PREVENTIVE,
                "Every 250 hours",
                LocalDate.of(2026, 7, 10),
                LocalDate.of(2026, 8, 5),
                "Wilfried Y."
        );

        testSchedule = new MaintenanceSchedule();
        testSchedule.setId(1L);
        testSchedule.setEquipment(testEquipment);
        testSchedule.setType(MaintenanceType.PREVENTIVE);
        testSchedule.setFrequency("Every 250 hours");
        testSchedule.setLastCompleted(LocalDate.of(2026, 7, 10));
        testSchedule.setNextDue(LocalDate.of(2026, 8, 5));
        testSchedule.setOperator("Wilfried Y.");
    }

    @Test
    void testAddMaintenanceSchedule_Success() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(testEquipment));
        when(maintenanceScheduleRepository.save(any(MaintenanceSchedule.class))).thenAnswer(invocation -> {
            MaintenanceSchedule saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        var result = maintenanceScheduleService.addMaintenanceSchedule(validRequest);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals(1L, result.equipmentId());
        assertEquals(MaintenanceType.PREVENTIVE, result.type());
        assertEquals("Every 250 hours", result.frequency());
        assertEquals(LocalDate.of(2026, 7, 10), result.lastCompleted());
        assertEquals(LocalDate.of(2026, 8, 5), result.nextDue());
        assertEquals("Wilfried Y.", result.operator());
    }

    @Test
    void testAddMaintenanceSchedule_EquipmentNotFound() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EquipmentNotFoundException.class, () -> maintenanceScheduleService.addMaintenanceSchedule(validRequest));
    }

    @Test
    void testAddMaintenanceSchedule_shouldThrowClearError_whenEquipmentDeletedConcurrently() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(testEquipment));
        doThrow(new DataIntegrityViolationException(
                "insert or update on table \"maintenance_schedule\" violates foreign key constraint"))
                .when(maintenanceScheduleRepository).save(any(MaintenanceSchedule.class));
        when(equipmentRepository.existsById(1L)).thenReturn(false);

        EquipmentNotFoundException exception = assertThrows(EquipmentNotFoundException.class,
                () -> maintenanceScheduleService.addMaintenanceSchedule(validRequest));

        assertEquals("Equipment with ID 1 not found", exception.getMessage());
    }

    @Test
    void testAddMaintenanceSchedule_nextDueBeforeLastCompleted() {
        MaintenanceScheduleCreationRequest incoherentRequest = new MaintenanceScheduleCreationRequest(
                1L,
                MaintenanceType.CORRECTIVE,
                "N/A",
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 5),
                null
        );
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(testEquipment));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> maintenanceScheduleService.addMaintenanceSchedule(incoherentRequest));

        assertTrue(exception.getMessage().contains("Next due date cannot be before"));
    }

    @Test
    void testObtainAllMaintenanceSchedules_Success() {
        Pageable pageable = mock(Pageable.class);
        when(maintenanceScheduleRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(Arrays.asList(testSchedule)));

        var result = maintenanceScheduleService.obtainAllMaintenanceSchedules(pageable);

        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).equipmentId());
    }

    @Test
    void testObtainMaintenanceScheduleById_Success() {
        when(maintenanceScheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));

        var result = maintenanceScheduleService.obtainMaintenanceScheduleById(1L);

        assertEquals(1L, result.id());
        assertEquals(MaintenanceType.PREVENTIVE, result.type());
    }

    @Test
    void testObtainMaintenanceScheduleById_NotFound() {
        when(maintenanceScheduleRepository.findById(999L)).thenReturn(Optional.empty());

        MaintenanceScheduleNotFoundException exception = assertThrows(MaintenanceScheduleNotFoundException.class,
                () -> maintenanceScheduleService.obtainMaintenanceScheduleById(999L));

        assertTrue(exception.getMessage().contains("not found with id: 999"));
    }

    @Test
    void testUpdateMaintenanceSchedule_Success() {
        MaintenanceScheduleUpdateRequest updateRequest = new MaintenanceScheduleUpdateRequest(
                null,
                MaintenanceType.CORRECTIVE,
                null,
                null,
                LocalDate.of(2026, 9, 1),
                "Abdoul S."
        );
        when(maintenanceScheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));
        when(maintenanceScheduleRepository.saveAndFlush(testSchedule)).thenReturn(testSchedule);

        var result = maintenanceScheduleService.updateMaintenanceSchedule(1L, updateRequest);

        assertEquals(MaintenanceType.CORRECTIVE, result.type());
        assertEquals(LocalDate.of(2026, 9, 1), result.nextDue());
        assertEquals("Abdoul S.", result.operator());
    }

    @Test
    void testUpdateMaintenanceSchedule_NotFound() {
        when(maintenanceScheduleRepository.findById(999L)).thenReturn(Optional.empty());

        MaintenanceScheduleUpdateRequest updateRequest = new MaintenanceScheduleUpdateRequest(
                null, null, null, null, null, null);

        assertThrows(MaintenanceScheduleNotFoundException.class,
                () -> maintenanceScheduleService.updateMaintenanceSchedule(999L, updateRequest));
    }

    @Test
    void testUpdateMaintenanceSchedule_equipmentNotFound() {
        MaintenanceScheduleUpdateRequest updateRequest = new MaintenanceScheduleUpdateRequest(
                2L, null, null, null, null, null);
        when(maintenanceScheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));
        when(equipmentRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(EquipmentNotFoundException.class,
                () -> maintenanceScheduleService.updateMaintenanceSchedule(1L, updateRequest));
    }

    @Test
    void testUpdateMaintenanceSchedule_incoherentDatesAgainstStoredValues() {
        MaintenanceScheduleUpdateRequest updateRequest = new MaintenanceScheduleUpdateRequest(
                null, null, null, null, LocalDate.of(2026, 7, 1), null);
        when(maintenanceScheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> maintenanceScheduleService.updateMaintenanceSchedule(1L, updateRequest));

        assertTrue(exception.getMessage().contains("Next due date cannot be before"));
    }

    @Test
    void testDeleteMaintenanceSchedule_Success() {
        when(maintenanceScheduleRepository.deleteMaintenanceScheduleById(1L)).thenReturn(1);

        maintenanceScheduleService.deleteMaintenanceSchedule(1L);

        verify(maintenanceScheduleRepository).deleteMaintenanceScheduleById(1L);
    }

    @Test
    void testDeleteMaintenanceSchedule_NotFound() {
        when(maintenanceScheduleRepository.deleteMaintenanceScheduleById(999L)).thenReturn(0);

        MaintenanceScheduleNotFoundException exception = assertThrows(MaintenanceScheduleNotFoundException.class,
                () -> maintenanceScheduleService.deleteMaintenanceSchedule(999L));

        assertTrue(exception.getMessage().contains("not found with id: 999"));
    }

    @Test
    void testDeleteMaintenanceSchedule_whenDeletedConcurrently() {
        when(maintenanceScheduleRepository.deleteMaintenanceScheduleById(5L)).thenReturn(0);

        assertThrows(MaintenanceScheduleNotFoundException.class,
                () -> maintenanceScheduleService.deleteMaintenanceSchedule(5L));
    }
}
