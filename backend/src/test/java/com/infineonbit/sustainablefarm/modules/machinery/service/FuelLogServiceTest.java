package com.infineonbit.sustainablefarm.modules.machinery.service;

import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.FuelLogCreationRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.FuelLogUpdateRequest;
import com.infineonbit.sustainablefarm.modules.machinery.entity.Equipment;
import com.infineonbit.sustainablefarm.modules.machinery.entity.FuelLog;
import com.infineonbit.sustainablefarm.modules.machinery.exception.EquipmentNotFoundException;
import com.infineonbit.sustainablefarm.modules.machinery.exception.FuelLogNotFoundException;
import com.infineonbit.sustainablefarm.modules.machinery.repository.EquipmentRepository;
import com.infineonbit.sustainablefarm.modules.machinery.repository.FuelLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
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
class FuelLogServiceTest {

    @Mock
    private FuelLogRepository fuelLogRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @InjectMocks
    private FuelLogService fuelLogService;

    private Equipment testEquipment;
    private FuelLog testFuelLog;
    private FuelLogCreationRequest validRequest;

    @BeforeEach
    void setUp() {
        testEquipment = new Equipment();
        testEquipment.setId(1L);

        validRequest = new FuelLogCreationRequest(
                1L,
                LocalDate.of(2026, 7, 22),
                new BigDecimal("25.00"),
                new BigDecimal("37.50")
        );

        testFuelLog = new FuelLog();
        testFuelLog.setId(1L);
        testFuelLog.setEquipment(testEquipment);
        testFuelLog.setDate(LocalDate.of(2026, 7, 22));
        testFuelLog.setLiters(new BigDecimal("25.00"));
        testFuelLog.setCost(new BigDecimal("37.50"));
    }

    @Test
    void testAddFuelLog_Success() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(testEquipment));
        when(fuelLogRepository.save(any(FuelLog.class))).thenAnswer(invocation -> {
            FuelLog saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        var result = fuelLogService.addFuelLog(validRequest);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals(1L, result.equipmentId());
        assertEquals(LocalDate.of(2026, 7, 22), result.date());
        assertEquals(new BigDecimal("25.00"), result.liters());
        assertEquals(new BigDecimal("37.50"), result.cost());
    }

    @Test
    void testAddFuelLog_EquipmentNotFound() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EquipmentNotFoundException.class, () -> fuelLogService.addFuelLog(validRequest));
    }

    @Test
    void testAddFuelLog_shouldThrowClearError_whenEquipmentDeletedConcurrently() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(testEquipment));
        doThrow(new DataIntegrityViolationException(
                "insert or update on table \"fuel_log\" violates foreign key constraint"))
                .when(fuelLogRepository).save(any(FuelLog.class));
        when(equipmentRepository.existsById(1L)).thenReturn(false);

        EquipmentNotFoundException exception = assertThrows(EquipmentNotFoundException.class,
                () -> fuelLogService.addFuelLog(validRequest));

        assertEquals("Equipment with ID 1 not found", exception.getMessage());
    }

    @Test
    void testObtainAllFuelLogs_Success() {
        Pageable pageable = mock(Pageable.class);
        when(fuelLogRepository.findAll(pageable)).thenReturn(new PageImpl<>(Arrays.asList(testFuelLog)));

        var result = fuelLogService.obtainAllFuelLogs(pageable);

        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).equipmentId());
    }

    @Test
    void testObtainFuelLogById_Success() {
        when(fuelLogRepository.findById(1L)).thenReturn(Optional.of(testFuelLog));

        var result = fuelLogService.obtainFuelLogById(1L);

        assertEquals(1L, result.id());
        assertEquals(new BigDecimal("25.00"), result.liters());
    }

    @Test
    void testObtainFuelLogById_NotFound() {
        when(fuelLogRepository.findById(999L)).thenReturn(Optional.empty());

        FuelLogNotFoundException exception = assertThrows(FuelLogNotFoundException.class,
                () -> fuelLogService.obtainFuelLogById(999L));

        assertTrue(exception.getMessage().contains("not found with id: 999"));
    }

    @Test
    void testUpdateFuelLog_Success() {
        FuelLogUpdateRequest updateRequest = new FuelLogUpdateRequest(
                null,
                null,
                new BigDecimal("30.00"),
                new BigDecimal("45.00")
        );
        when(fuelLogRepository.findById(1L)).thenReturn(Optional.of(testFuelLog));
        when(fuelLogRepository.saveAndFlush(testFuelLog)).thenReturn(testFuelLog);

        var result = fuelLogService.updateFuelLog(1L, updateRequest);

        assertEquals(new BigDecimal("30.00"), result.liters());
        assertEquals(new BigDecimal("45.00"), result.cost());
    }

    @Test
    void testUpdateFuelLog_NotFound() {
        when(fuelLogRepository.findById(999L)).thenReturn(Optional.empty());

        FuelLogUpdateRequest updateRequest = new FuelLogUpdateRequest(null, null, null, null);

        assertThrows(FuelLogNotFoundException.class, () -> fuelLogService.updateFuelLog(999L, updateRequest));
    }

    @Test
    void testUpdateFuelLog_equipmentNotFound() {
        FuelLogUpdateRequest updateRequest = new FuelLogUpdateRequest(2L, null, null, null);
        when(fuelLogRepository.findById(1L)).thenReturn(Optional.of(testFuelLog));
        when(equipmentRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(EquipmentNotFoundException.class, () -> fuelLogService.updateFuelLog(1L, updateRequest));
    }

    @Test
    void testDeleteFuelLog_Success() {
        when(fuelLogRepository.deleteFuelLogById(1L)).thenReturn(1);

        fuelLogService.deleteFuelLog(1L);

        verify(fuelLogRepository).deleteFuelLogById(1L);
    }

    @Test
    void testDeleteFuelLog_NotFound() {
        when(fuelLogRepository.deleteFuelLogById(999L)).thenReturn(0);

        assertThrows(FuelLogNotFoundException.class, () -> fuelLogService.deleteFuelLog(999L));
    }
}
