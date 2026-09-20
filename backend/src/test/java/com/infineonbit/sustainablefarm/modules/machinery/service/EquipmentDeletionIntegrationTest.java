package com.infineonbit.sustainablefarm.modules.machinery.service;

import com.infineonbit.sustainablefarm.modules.machinery.entity.Equipment;
import com.infineonbit.sustainablefarm.modules.machinery.entity.SparePart;
import com.infineonbit.sustainablefarm.modules.machinery.enums.Category;
import com.infineonbit.sustainablefarm.modules.machinery.enums.Stage;
import com.infineonbit.sustainablefarm.modules.machinery.enums.Status;
import com.infineonbit.sustainablefarm.modules.machinery.repository.EquipmentRepository;
import com.infineonbit.sustainablefarm.modules.machinery.repository.SparePartRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(EquipmentService.class)
class EquipmentDeletionIntegrationTest {

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private SparePartRepository sparePartRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void deletingEquipment_shouldPreserveItsSpareParts() {
        Equipment equipment = new Equipment();
        equipment.setName("Tractor");
        equipment.setCategory(Category.AGRICULTURAL_MACHINERY);
        equipment.setStage(Stage.CULTIVATION);
        equipment.setStatus(Status.OPERATIONAL);
        equipment = equipmentRepository.save(equipment);

        SparePart attached = new SparePart();
        attached.setName("Air Filter");
        attached.setQuantity(10);
        attached.setReorderThreshold(2);
        attached.setUnitCost(new BigDecimal("25.99"));
        attached.setEquipment(equipment);
        attached = sparePartRepository.save(attached);

        equipmentService.deleteEquipment(equipment.getId());
        entityManager.flush();
        entityManager.clear();

        assertFalse(equipmentRepository.existsById(equipment.getId()));

        SparePart detached = sparePartRepository.findById(attached.getId()).orElseThrow();
        assertEquals("Air Filter", detached.getName());
        assertEquals(10, detached.getQuantity());
        assertNull(detached.getEquipment());
    }

    @Test
    void deletingEquipment_shouldNotTouchStandaloneSpareParts() {
        SparePart standalone = new SparePart();
        standalone.setName("General Bolt");
        standalone.setQuantity(100);
        standalone.setReorderThreshold(20);
        standalone.setUnitCost(new BigDecimal("0.50"));
        standalone = sparePartRepository.save(standalone);

        Equipment equipment = new Equipment();
        equipment.setName("Harvester");
        equipment.setCategory(Category.AGRICULTURAL_MACHINERY);
        equipment.setStage(Stage.HARVEST);
        equipment.setStatus(Status.OPERATIONAL);
        equipment = equipmentRepository.save(equipment);

        equipmentService.deleteEquipment(equipment.getId());
        entityManager.flush();
        entityManager.clear();

        SparePart reloaded = sparePartRepository.findById(standalone.getId()).orElseThrow();
        assertEquals("General Bolt", reloaded.getName());
        assertNull(reloaded.getEquipment());
        assertTrue(equipmentRepository.findById(equipment.getId()).isEmpty());
    }
}
