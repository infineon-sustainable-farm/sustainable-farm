package com.infineonbit.sustainablefarm.modules.machinery.service;

import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.SparePartCreationRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.SparePartUpdateRequest;
import com.infineonbit.sustainablefarm.modules.machinery.entity.Equipment;
import com.infineonbit.sustainablefarm.modules.machinery.entity.SparePart;
import com.infineonbit.sustainablefarm.modules.machinery.enums.Category;
import com.infineonbit.sustainablefarm.modules.machinery.enums.Stage;
import com.infineonbit.sustainablefarm.modules.machinery.enums.Status;
import com.infineonbit.sustainablefarm.modules.machinery.exception.SparePartNotFoundException;
import com.infineonbit.sustainablefarm.modules.machinery.repository.EquipmentRepository;
import com.infineonbit.sustainablefarm.modules.machinery.repository.SparePartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SparePartServiceTest {

    @Mock
    private SparePartRepository sparePartRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @InjectMocks
    private SparePartService sparePartService;

    private Equipment testEquipment;
    private SparePartCreationRequest validSparePartRequest;
    private SparePart testSparePart;

    @BeforeEach
    void setUp() {
        testEquipment = new Equipment();
        testEquipment.setId(1L);
        testEquipment.setName("John Deere 8R");
        testEquipment.setCategory(Category.AGRICULTURAL_MACHINERY);
        testEquipment.setStage(Stage.PROCESSING);
        testEquipment.setStatus(Status.OPERATIONAL);

        validSparePartRequest = new SparePartCreationRequest(
                "Air Filter",
                50,
                10,
                new BigDecimal("25.99"),
                1L
        );

        testSparePart = new SparePart();
        testSparePart.setId(1L);
        testSparePart.setName("Air Filter");
        testSparePart.setQuantity(50);
        testSparePart.setReorderThreshold(10);
        testSparePart.setUnitCost(new BigDecimal("25.99"));
        testSparePart.setEquipment(testEquipment);
    }

    @Test
    void testAddSparePart_Success() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(testEquipment));
        when(sparePartRepository.existsByName("Air Filter")).thenReturn(false);
        when(sparePartRepository.save(any(SparePart.class))).thenReturn(testSparePart);

        var result = sparePartService.addSparePart(validSparePartRequest);

        assertNotNull(result);
        assertEquals("Air Filter", result.name());
        assertEquals(50, result.quantity());
        assertEquals(10, result.reorderThreshold());
        assertEquals(new BigDecimal("25.99"), result.unitCost());
        assertEquals(1L, result.equipmentId());

        verify(sparePartRepository).save(any(SparePart.class));
    }

    @Test
    void testAddSparePart_NameAlreadyExists() {
        when(sparePartRepository.existsByName("Air Filter")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            sparePartService.addSparePart(validSparePartRequest);
        });

        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    void testAddSparePart_EquipmentNotFound() {
        when(sparePartRepository.existsByName("Air Filter")).thenReturn(false);
        when(equipmentRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            sparePartService.addSparePart(validSparePartRequest);
        });

        assertTrue(exception.getMessage().contains("Equipment not found with id: 1"));
    }

    @Test
    void testObtainAllSpareParts_Success() {
        List<SparePart> spareParts = Arrays.asList(testSparePart);
        org.springframework.data.domain.Pageable mockPageable = org.mockito.Mockito.mock(org.springframework.data.domain.Pageable.class);

        when(sparePartRepository.findAll(org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(spareParts));

        var result = sparePartService.obtainAllSpareParts(mockPageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Air Filter", result.getContent().get(0).name());
    }

    @Test
    void testObtainSparePartById_Success() {
        when(sparePartRepository.findById(1L)).thenReturn(Optional.of(testSparePart));

        var result = sparePartService.obtainSparePartById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Air Filter", result.name());
        assertEquals(50, result.quantity());
    }

    @Test
    void testObtainSparePartById_NotFound() {
        when(sparePartRepository.findById(999L)).thenReturn(Optional.empty());

        SparePartNotFoundException exception = assertThrows(SparePartNotFoundException.class, () -> {
            sparePartService.obtainSparePartById(999L);
        });

        assertTrue(exception.getMessage().contains("not found with id: 999"));
    }

    @Test
    void testUpdateSparePart_Success() {
        SparePartUpdateRequest updateRequest = new SparePartUpdateRequest(
                "Updated Air Filter",
                60,
                15,
                new BigDecimal("29.99"),
                null
        );

        when(sparePartRepository.findById(1L)).thenReturn(Optional.of(testSparePart));
        when(sparePartRepository.save(any(SparePart.class))).thenReturn(testSparePart);

        var result = sparePartService.updateSparePart(1L, updateRequest);

        assertNotNull(result);
        assertEquals("Updated Air Filter", result.name());
        assertEquals(60, result.quantity());
        assertEquals(new BigDecimal("29.99"), result.unitCost());
    }

    @Test
    void testUpdateSparePartQuantity_Success() {
        SparePart updatedSparePart = new SparePart();
        updatedSparePart.setId(1L);
        updatedSparePart.setName("Air Filter");
        updatedSparePart.setQuantity(45);
        updatedSparePart.setReorderThreshold(10);
        updatedSparePart.setUnitCost(new BigDecimal("25.99"));
        updatedSparePart.setEquipment(testSparePart.getEquipment());

        when(sparePartRepository.findById(1L)).thenReturn(Optional.of(testSparePart));
        when(sparePartRepository.updateSparePartQuantityById(1L, 45)).thenReturn(1);
        when(sparePartRepository.findById(1L)).thenReturn(Optional.of(updatedSparePart));

        var result = sparePartService.updateSparePartQuantity(1L, 45);

        assertNotNull(result);
        assertEquals(45, result.quantity());
    }

    @Test
    void testUpdateSparePartUnitCost_Success() {
        SparePart updatedSparePart = new SparePart();
        updatedSparePart.setId(1L);
        updatedSparePart.setName("Air Filter");
        updatedSparePart.setQuantity(50);
        updatedSparePart.setReorderThreshold(10);
        updatedSparePart.setUnitCost(new BigDecimal("24.99"));
        updatedSparePart.setEquipment(testSparePart.getEquipment());

        when(sparePartRepository.findById(1L)).thenReturn(Optional.of(testSparePart));
        when(sparePartRepository.updateSparePartUnitCostById(1L, new BigDecimal("24.99"))).thenReturn(1);
        when(sparePartRepository.findById(1L)).thenReturn(Optional.of(updatedSparePart));

        var result = sparePartService.updateSparePartUnitCost(1L, new BigDecimal("24.99"));

        assertNotNull(result);
        assertEquals(new BigDecimal("24.99"), result.unitCost());
    }

    @Test
    void testObtainSparePartsByEquipment_Success() {
        when(sparePartRepository.findByEquipmentId(1L)).thenReturn(Arrays.asList(testSparePart));

        var result = sparePartService.obtainSparePartsByEquipment(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Air Filter", result.get(0).name());
    }

    @Test
    void testObtainLowStockSpareParts_Success() {
        when(sparePartRepository.findLowStockSpareParts()).thenReturn(Arrays.asList(testSparePart));

        var result = sparePartService.obtainLowStockSpareParts();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Air Filter", result.get(0).name());
        assertEquals(1L, result.get(0).equipmentId());
    }

    @Test
    void testDeleteSparePart_Success() {
        when(sparePartRepository.deleteSparePartById(1L)).thenReturn(1);

        sparePartService.deleteSparePart(1L);

        verify(sparePartRepository).deleteSparePartById(1L);
    }

    @Test
    void testDeleteSparePart_NotFound() {
        when(sparePartRepository.deleteSparePartById(999L)).thenReturn(0);

        SparePartNotFoundException exception = assertThrows(SparePartNotFoundException.class, () -> {
            sparePartService.deleteSparePart(999L);
        });

        assertTrue(exception.getMessage().contains("not found with id: 999"));
    }

    @Test
    void testAddSparePart_shouldThrowClearError_whenDuplicateNameInsertRaces() {
        when(sparePartRepository.existsByName("Air Filter"))
                .thenReturn(false)
                .thenReturn(true);
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(testEquipment));
        doThrow(new org.springframework.dao.DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"spare_part_name_key\""))
                .when(sparePartRepository).save(any(SparePart.class));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            sparePartService.addSparePart(validSparePartRequest);
        });

        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    void testAddSparePart_shouldThrowClearError_whenEquipmentDeletedConcurrently() {
        when(sparePartRepository.existsByName("Air Filter")).thenReturn(false);
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(testEquipment));
        doThrow(new org.springframework.dao.DataIntegrityViolationException(
                "insert or update on table \"spare_part\" violates foreign key constraint \"fk_spare_part_equipment\""))
                .when(sparePartRepository).save(any(SparePart.class));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            sparePartService.addSparePart(validSparePartRequest);
        });

        assertTrue(exception.getMessage().contains("Equipment not found with id: 1"));
    }

    @Test
    void testUpdateSparePart_shouldThrowClearError_whenEquipmentDeletedConcurrently() {
        SparePartUpdateRequest updateRequest = new SparePartUpdateRequest(
                "Air Filter",
                50,
                10,
                new BigDecimal("25.99"),
                2L
        );
        Equipment otherEquipment = new Equipment();
        otherEquipment.setId(2L);

        when(sparePartRepository.findById(1L)).thenReturn(Optional.of(testSparePart));
        when(equipmentRepository.findById(2L)).thenReturn(Optional.of(otherEquipment));
        doThrow(new org.springframework.dao.DataIntegrityViolationException(
                "update on table \"spare_part\" violates foreign key constraint \"fk_spare_part_equipment\""))
                .when(sparePartRepository).save(any(SparePart.class));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            sparePartService.updateSparePart(1L, updateRequest);
        });

        assertTrue(exception.getMessage().contains("Equipment not found with id: 2"));
    }

    @Test
    void testStandaloneSparePart() {
        SparePartCreationRequest standaloneRequest = new SparePartCreationRequest(
                "General Bolt",
                100,
                20,
                new BigDecimal("0.50"),
                null
        );

        when(sparePartRepository.existsByName("General Bolt")).thenReturn(false);
        when(sparePartRepository.save(any(SparePart.class))).thenAnswer(invocation -> {
            SparePart savedPart = invocation.getArgument(0);
            savedPart.setId(1L);
            return savedPart;
        });

        var result = sparePartService.addSparePart(standaloneRequest);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("General Bolt", result.name());
        assertEquals(null, result.equipmentId());
    }
}
