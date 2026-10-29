package com.infineonbit.sustainablefarm.modules.machinery.service;

import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.RepairLogCreationRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.RepairLogUpdateRequest;
import com.infineonbit.sustainablefarm.modules.machinery.entity.Equipment;
import com.infineonbit.sustainablefarm.modules.machinery.entity.RepairLog;
import com.infineonbit.sustainablefarm.modules.machinery.exception.EquipmentNotFoundException;
import com.infineonbit.sustainablefarm.modules.machinery.exception.RepairLogNotFoundException;
import com.infineonbit.sustainablefarm.modules.machinery.repository.EquipmentRepository;
import com.infineonbit.sustainablefarm.modules.machinery.repository.RepairLogRepository;
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
class RepairLogServiceTest {

    @Mock
    private RepairLogRepository repairLogRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @InjectMocks
    private RepairLogService repairLogService;

    private Equipment testEquipment;
    private RepairLog testRepairLog;
    private RepairLogCreationRequest validRequest;

    @BeforeEach
    void setUp() {
        testEquipment = new Equipment();
        testEquipment.setId(1L);

        validRequest = new RepairLogCreationRequest(
                1L,
                LocalDate.of(2026, 7, 22),
                "Heating element malfunction",
                new BigDecimal("14.00"),
                new BigDecimal("150.00"),
                "Maintenance Team A"
        );

        testRepairLog = new RepairLog();
        testRepairLog.setId(1L);
        testRepairLog.setEquipment(testEquipment);
        testRepairLog.setDate(LocalDate.of(2026, 7, 22));
        testRepairLog.setIssue("Heating element malfunction");
        testRepairLog.setDowntime(new BigDecimal("14.00"));
        testRepairLog.setCost(new BigDecimal("150.00"));
        testRepairLog.setTechnician("Maintenance Team A");
    }

    @Test
    void testAddRepairLog_Success() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(testEquipment));
        when(repairLogRepository.save(any(RepairLog.class))).thenAnswer(invocation -> {
            RepairLog saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        var result = repairLogService.addRepairLog(validRequest);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals(1L, result.equipmentId());
        assertEquals("Heating element malfunction", result.issue());
        assertEquals(new BigDecimal("14.00"), result.downtime());
        assertEquals(new BigDecimal("150.00"), result.cost());
        assertEquals("Maintenance Team A", result.technician());
    }

    @Test
    void testAddRepairLog_EquipmentNotFound() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EquipmentNotFoundException.class, () -> repairLogService.addRepairLog(validRequest));
    }

    @Test
    void testAddRepairLog_blankIssue() {
        RepairLogCreationRequest blankIssueRequest = new RepairLogCreationRequest(
                1L,
                LocalDate.of(2026, 7, 22),
                "   ",
                null,
                null,
                null
        );
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(testEquipment));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> repairLogService.addRepairLog(blankIssueRequest));

        assertTrue(exception.getMessage().contains("Issue description is required"));
    }

    @Test
    void testAddRepairLog_shouldThrowClearError_whenEquipmentDeletedConcurrently() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(testEquipment));
        doThrow(new DataIntegrityViolationException(
                "insert or update on table \"repair_log\" violates foreign key constraint"))
                .when(repairLogRepository).save(any(RepairLog.class));
        when(equipmentRepository.existsById(1L)).thenReturn(false);

        EquipmentNotFoundException exception = assertThrows(EquipmentNotFoundException.class,
                () -> repairLogService.addRepairLog(validRequest));

        assertEquals("Equipment with ID 1 not found", exception.getMessage());
    }

    @Test
    void testObtainAllRepairLogs_Success() {
        Pageable pageable = mock(Pageable.class);
        when(repairLogRepository.findAll(pageable)).thenReturn(new PageImpl<>(Arrays.asList(testRepairLog)));

        var result = repairLogService.obtainAllRepairLogs(pageable);

        assertEquals(1, result.getContent().size());
        assertEquals("Heating element malfunction", result.getContent().get(0).issue());
    }

    @Test
    void testObtainRepairLogById_Success() {
        when(repairLogRepository.findById(1L)).thenReturn(Optional.of(testRepairLog));

        var result = repairLogService.obtainRepairLogById(1L);

        assertEquals(1L, result.id());
        assertEquals("Maintenance Team A", result.technician());
    }

    @Test
    void testObtainRepairLogById_NotFound() {
        when(repairLogRepository.findById(999L)).thenReturn(Optional.empty());

        RepairLogNotFoundException exception = assertThrows(RepairLogNotFoundException.class,
                () -> repairLogService.obtainRepairLogById(999L));

        assertTrue(exception.getMessage().contains("not found with id: 999"));
    }

    @Test
    void testUpdateRepairLog_Success() {
        RepairLogUpdateRequest updateRequest = new RepairLogUpdateRequest(
                null,
                null,
                "Blade drive belt snapped",
                new BigDecimal("48.00"),
                new BigDecimal("85.00"),
                null
        );
        when(repairLogRepository.findById(1L)).thenReturn(Optional.of(testRepairLog));
        when(repairLogRepository.saveAndFlush(testRepairLog)).thenReturn(testRepairLog);

        var result = repairLogService.updateRepairLog(1L, updateRequest);

        assertEquals("Blade drive belt snapped", result.issue());
        assertEquals(new BigDecimal("48.00"), result.downtime());
        assertEquals(new BigDecimal("85.00"), result.cost());
    }

    @Test
    void testUpdateRepairLog_blankIssue() {
        RepairLogUpdateRequest updateRequest = new RepairLogUpdateRequest(
                null, null, "  ", null, null, null);
        when(repairLogRepository.findById(1L)).thenReturn(Optional.of(testRepairLog));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> repairLogService.updateRepairLog(1L, updateRequest));

        assertTrue(exception.getMessage().contains("Issue description cannot be blank"));
    }

    @Test
    void testUpdateRepairLog_NotFound() {
        when(repairLogRepository.findById(999L)).thenReturn(Optional.empty());

        RepairLogUpdateRequest updateRequest = new RepairLogUpdateRequest(null, null, null, null, null, null);

        assertThrows(RepairLogNotFoundException.class, () -> repairLogService.updateRepairLog(999L, updateRequest));
    }

    @Test
    void testUpdateRepairLog_equipmentNotFound() {
        RepairLogUpdateRequest updateRequest = new RepairLogUpdateRequest(2L, null, null, null, null, null);
        when(repairLogRepository.findById(1L)).thenReturn(Optional.of(testRepairLog));
        when(equipmentRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(EquipmentNotFoundException.class, () -> repairLogService.updateRepairLog(1L, updateRequest));
    }

    @Test
    void testDeleteRepairLog_Success() {
        when(repairLogRepository.deleteRepairLogById(1L)).thenReturn(1);

        repairLogService.deleteRepairLog(1L);

        verify(repairLogRepository).deleteRepairLogById(1L);
    }

    @Test
    void testDeleteRepairLog_NotFound() {
        when(repairLogRepository.deleteRepairLogById(999L)).thenReturn(0);

        assertThrows(RepairLogNotFoundException.class, () -> repairLogService.deleteRepairLog(999L));
    }
}
