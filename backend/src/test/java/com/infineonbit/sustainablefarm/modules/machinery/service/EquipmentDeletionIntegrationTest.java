package com.infineonbit.sustainablefarm.modules.machinery.service;

import com.infineonbit.sustainablefarm.modules.machinery.entity.Equipment;
import com.infineonbit.sustainablefarm.modules.machinery.entity.FuelLog;
import com.infineonbit.sustainablefarm.modules.machinery.entity.MaintenanceSchedule;
import com.infineonbit.sustainablefarm.modules.machinery.entity.RepairLog;
import com.infineonbit.sustainablefarm.modules.machinery.entity.SparePart;
import com.infineonbit.sustainablefarm.modules.machinery.entity.UsageLog;
import com.infineonbit.sustainablefarm.modules.machinery.enums.Category;
import com.infineonbit.sustainablefarm.modules.machinery.enums.MaintenanceType;
import com.infineonbit.sustainablefarm.modules.machinery.enums.Stage;
import com.infineonbit.sustainablefarm.modules.machinery.enums.Status;
import com.infineonbit.sustainablefarm.modules.machinery.repository.EquipmentRepository;
import com.infineonbit.sustainablefarm.modules.machinery.repository.FuelLogRepository;
import com.infineonbit.sustainablefarm.modules.machinery.repository.MaintenanceScheduleRepository;
import com.infineonbit.sustainablefarm.modules.machinery.repository.RepairLogRepository;
import com.infineonbit.sustainablefarm.modules.machinery.repository.SparePartRepository;
import com.infineonbit.sustainablefarm.modules.machinery.repository.UsageLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;

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
    private MaintenanceScheduleRepository maintenanceScheduleRepository;

    @Autowired
    private UsageLogRepository usageLogRepository;

    @Autowired
    private FuelLogRepository fuelLogRepository;

    @Autowired
    private RepairLogRepository repairLogRepository;

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

    @Test
    void deletingEquipment_shouldPreserveMaintenanceUsageFuelAndRepairLogs() {
        Equipment equipment = new Equipment();
        equipment.setName("Dryer");
        equipment.setCategory(Category.PROCESSING_MACHINERY);
        equipment.setStage(Stage.DRYING);
        equipment.setStatus(Status.UNDER_MAINTENANCE);
        equipment = equipmentRepository.save(equipment);

        MaintenanceSchedule schedule = new MaintenanceSchedule();
        schedule.setEquipment(equipment);
        schedule.setType(MaintenanceType.PREVENTIVE);
        schedule.setFrequency("Every 3 months");
        schedule.setLastCompleted(LocalDate.of(2026, 5, 15));
        schedule.setNextDue(LocalDate.of(2026, 8, 15));
        schedule.setOperator("Jean-Louis R.");
        schedule = maintenanceScheduleRepository.save(schedule);

        UsageLog usageLog = new UsageLog();
        usageLog.setEquipment(equipment);
        usageLog.setDate(LocalDate.of(2026, 7, 23));
        usageLog.setHoursUsed(new BigDecimal("6.50"));
        usageLog.setOperator("Wilfried Y.");
        usageLog.setNotes("Morning drying cycle");
        usageLog = usageLogRepository.save(usageLog);

        FuelLog fuelLog = new FuelLog();
        fuelLog.setEquipment(equipment);
        fuelLog.setDate(LocalDate.of(2026, 7, 22));
        fuelLog.setLiters(new BigDecimal("25.00"));
        fuelLog.setCost(new BigDecimal("37.50"));
        fuelLog = fuelLogRepository.save(fuelLog);

        RepairLog repairLog = new RepairLog();
        repairLog.setEquipment(equipment);
        repairLog.setDate(LocalDate.of(2026, 7, 22));
        repairLog.setIssue("Heating element malfunction");
        repairLog.setDowntime(new BigDecimal("14.00"));
        repairLog.setCost(new BigDecimal("150.00"));
        repairLog.setTechnician("Maintenance Team A");
        repairLog = repairLogRepository.save(repairLog);

        Long equipmentId = equipment.getId();
        Long scheduleId = schedule.getId();
        Long usageLogId = usageLog.getId();
        Long fuelLogId = fuelLog.getId();
        Long repairLogId = repairLog.getId();

        equipmentService.deleteEquipment(equipmentId);
        entityManager.flush();
        entityManager.clear();

        assertFalse(equipmentRepository.existsById(equipmentId));

        MaintenanceSchedule detachedSchedule = maintenanceScheduleRepository.findById(scheduleId).orElseThrow();
        assertEquals(MaintenanceType.PREVENTIVE, detachedSchedule.getType());
        assertNull(detachedSchedule.getEquipment());

        UsageLog detachedUsageLog = usageLogRepository.findById(usageLogId).orElseThrow();
        assertEquals(new BigDecimal("6.50"), detachedUsageLog.getHoursUsed());
        assertNull(detachedUsageLog.getEquipment());

        FuelLog detachedFuelLog = fuelLogRepository.findById(fuelLogId).orElseThrow();
        assertEquals(new BigDecimal("37.50"), detachedFuelLog.getCost());
        assertNull(detachedFuelLog.getEquipment());

        RepairLog detachedRepairLog = repairLogRepository.findById(repairLogId).orElseThrow();
        assertEquals("Heating element malfunction", detachedRepairLog.getIssue());
        assertNull(detachedRepairLog.getEquipment());
    }
}
