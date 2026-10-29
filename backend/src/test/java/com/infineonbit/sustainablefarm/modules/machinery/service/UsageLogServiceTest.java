package com.infineonbit.sustainablefarm.modules.machinery.service;

import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.UsageLogCreationRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.UsageLogUpdateRequest;
import com.infineonbit.sustainablefarm.modules.machinery.entity.Equipment;
import com.infineonbit.sustainablefarm.modules.machinery.entity.UsageLog;
import com.infineonbit.sustainablefarm.modules.machinery.exception.EquipmentNotFoundException;
import com.infineonbit.sustainablefarm.modules.machinery.exception.UsageLogNotFoundException;
import com.infineonbit.sustainablefarm.modules.machinery.repository.EquipmentRepository;
import com.infineonbit.sustainablefarm.modules.machinery.repository.UsageLogRepository;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsageLogServiceTest {

    @Mock
    private UsageLogRepository usageLogRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @InjectMocks
    private UsageLogService usageLogService;

    private Equipment testEquipment;
    private UsageLog testUsageLog;
    private UsageLogCreationRequest validRequest;

    @BeforeEach
    void setUp() {
        testEquipment = new Equipment();
        testEquipment.setId(1L);

        validRequest = new UsageLogCreationRequest(
                1L,
                LocalDate.of(2026, 7, 23),
                new BigDecimal("6.50"),
                "Wilfried Y.",
                "Plowing field section B"
        );

        testUsageLog = new UsageLog();
        testUsageLog.setId(1L);
        testUsageLog.setEquipment(testEquipment);
        testUsageLog.setDate(LocalDate.of(2026, 7, 23));
        testUsageLog.setHoursUsed(new BigDecimal("6.50"));
        testUsageLog.setOperator("Wilfried Y.");
        testUsageLog.setNotes("Plowing field section B");
    }

    @Test
    void testAddUsageLog_Success() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(testEquipment));
        when(usageLogRepository.save(any(UsageLog.class))).thenAnswer(invocation -> {
            UsageLog saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        var result = usageLogService.addUsageLog(validRequest);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals(1L, result.equipmentId());
        assertEquals(LocalDate.of(2026, 7, 23), result.date());
        assertEquals(new BigDecimal("6.50"), result.hoursUsed());
        assertEquals("Wilfried Y.", result.operator());
        assertEquals("Plowing field section B", result.notes());
    }

    @Test
    void testAddUsageLog_EquipmentNotFound() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EquipmentNotFoundException.class, () -> usageLogService.addUsageLog(validRequest));
    }

    @Test
    void testAddUsageLog_shouldThrowClearError_whenEquipmentDeletedConcurrently() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(testEquipment));
        doThrow(new DataIntegrityViolationException(
                "insert or update on table \"usage_log\" violates foreign key constraint"))
                .when(usageLogRepository).save(any(UsageLog.class));
        when(equipmentRepository.existsById(1L)).thenReturn(false);

        EquipmentNotFoundException exception = assertThrows(EquipmentNotFoundException.class,
                () -> usageLogService.addUsageLog(validRequest));

        assertEquals("Equipment with ID 1 not found", exception.getMessage());
    }

    @Test
    void testObtainAllUsageLogs_Success() {
        Pageable pageable = mock(Pageable.class);
        when(usageLogRepository.findAll(pageable)).thenReturn(new PageImpl<>(Arrays.asList(testUsageLog)));

        var result = usageLogService.obtainAllUsageLogs(pageable);

        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).equipmentId());
    }

    @Test
    void testObtainUsageLogById_Success() {
        when(usageLogRepository.findById(1L)).thenReturn(Optional.of(testUsageLog));

        var result = usageLogService.obtainUsageLogById(1L);

        assertEquals(1L, result.id());
        assertEquals(new BigDecimal("6.50"), result.hoursUsed());
    }

    @Test
    void testObtainUsageLogById_NotFound() {
        when(usageLogRepository.findById(999L)).thenReturn(Optional.empty());

        UsageLogNotFoundException exception = assertThrows(UsageLogNotFoundException.class,
                () -> usageLogService.obtainUsageLogById(999L));

        assertTrue(exception.getMessage().contains("not found with id: 999"));
    }

    @Test
    void testUpdateUsageLog_Success() {
        UsageLogUpdateRequest updateRequest = new UsageLogUpdateRequest(
                null,
                null,
                new BigDecimal("8.00"),
                null,
                ""
        );
        when(usageLogRepository.findById(1L)).thenReturn(Optional.of(testUsageLog));
        when(usageLogRepository.saveAndFlush(testUsageLog)).thenReturn(testUsageLog);

        var result = usageLogService.updateUsageLog(1L, updateRequest);

        assertEquals(new BigDecimal("8.00"), result.hoursUsed());
        assertNull(result.notes());
    }

    @Test
    void testUpdateUsageLog_NotFound() {
        when(usageLogRepository.findById(999L)).thenReturn(Optional.empty());

        UsageLogUpdateRequest updateRequest = new UsageLogUpdateRequest(null, null, null, null, null);

        assertThrows(UsageLogNotFoundException.class, () -> usageLogService.updateUsageLog(999L, updateRequest));
    }

    @Test
    void testUpdateUsageLog_equipmentNotFound() {
        UsageLogUpdateRequest updateRequest = new UsageLogUpdateRequest(2L, null, null, null, null);
        when(usageLogRepository.findById(1L)).thenReturn(Optional.of(testUsageLog));
        when(equipmentRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(EquipmentNotFoundException.class, () -> usageLogService.updateUsageLog(1L, updateRequest));
    }

    @Test
    void testDeleteUsageLog_Success() {
        when(usageLogRepository.deleteUsageLogById(1L)).thenReturn(1);

        usageLogService.deleteUsageLog(1L);

        verify(usageLogRepository).deleteUsageLogById(1L);
    }

    @Test
    void testDeleteUsageLog_NotFound() {
        when(usageLogRepository.deleteUsageLogById(999L)).thenReturn(0);

        assertThrows(UsageLogNotFoundException.class, () -> usageLogService.deleteUsageLog(999L));
    }
}
