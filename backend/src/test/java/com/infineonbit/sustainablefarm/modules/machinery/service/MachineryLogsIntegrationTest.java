package com.infineonbit.sustainablefarm.modules.machinery.service;

import com.infineonbit.sustainablefarm.modules.staff.api.TestPeople;
import com.infineonbit.sustainablefarm.modules.machinery.entity.MaintenanceSchedule;
import com.infineonbit.sustainablefarm.modules.machinery.entity.RepairLog;
import com.infineonbit.sustainablefarm.modules.machinery.entity.UsageLog;
import com.infineonbit.sustainablefarm.core.exception.BadRequestException;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import com.infineonbit.sustainablefarm.modules.staff.repository.PersonRepository;
import com.infineonbit.sustainablefarm.modules.staff.service.PersonDirectoryService;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.FuelLogCreationRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.FuelLogUpdateRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.MaintenanceScheduleCreationRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.MaintenanceScheduleUpdateRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.RepairLogCreationRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.RepairLogUpdateRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.UsageLogCreationRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.UsageLogUpdateRequest;
import com.infineonbit.sustainablefarm.modules.machinery.entity.Equipment;
import com.infineonbit.sustainablefarm.modules.machinery.api.Category;
import com.infineonbit.sustainablefarm.modules.machinery.enums.MaintenanceType;
import com.infineonbit.sustainablefarm.modules.machinery.api.Stage;
import com.infineonbit.sustainablefarm.modules.machinery.api.Status;
import com.infineonbit.sustainablefarm.modules.machinery.exception.FuelLogNotFoundException;
import com.infineonbit.sustainablefarm.modules.machinery.exception.MaintenanceScheduleNotFoundException;
import com.infineonbit.sustainablefarm.modules.machinery.exception.RepairLogNotFoundException;
import com.infineonbit.sustainablefarm.modules.machinery.exception.UsageLogNotFoundException;
import com.infineonbit.sustainablefarm.modules.machinery.repository.EquipmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Import({
        MaintenanceScheduleService.class,
        UsageLogService.class,
        FuelLogService.class,
        RepairLogService.class,
        PersonDirectoryService.class
})
class MachineryLogsIntegrationTest {

    @Autowired
    private MaintenanceScheduleService maintenanceScheduleService;

    @Autowired
    private UsageLogService usageLogService;

    @Autowired
    private FuelLogService fuelLogService;

    @Autowired
    private RepairLogService repairLogService;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private PersonDirectory personDirectory;

    @Test
    void maintenanceScheduleLifecycle() {
        Equipment equipment = createEquipment("Tractor");

        var created = maintenanceScheduleService.addMaintenanceSchedule(new MaintenanceScheduleCreationRequest(
                equipment.getId(),
                MaintenanceType.PREVENTIVE,
                "Every 250 hours",
                LocalDate.of(2026, 7, 10),
                LocalDate.of(2026, 8, 5),
                TestPeople.idOf(personDirectory, "Wilfried Y.")
        ));

        assertEquals(equipment.getId(), created.equipmentId());
        assertEquals(MaintenanceType.PREVENTIVE, created.type());
        assertEquals(1, maintenanceScheduleService.obtainAllMaintenanceSchedules(PageRequest.of(0, 10)).getTotalElements());

        var updated = maintenanceScheduleService.updateMaintenanceSchedule(created.id(),
                new MaintenanceScheduleUpdateRequest(null, MaintenanceType.CORRECTIVE, null, null, null, TestPeople.idOf(personDirectory, "Abdoul S.")));

        assertEquals(MaintenanceType.CORRECTIVE, updated.type());
        assertEquals("Abdoul S.", updated.operator());

        maintenanceScheduleService.deleteMaintenanceSchedule(created.id());
        entityManager.flush();
        entityManager.clear();

        assertThrows(MaintenanceScheduleNotFoundException.class,
                () -> maintenanceScheduleService.obtainMaintenanceScheduleById(created.id()));
    }

    @Test
    void usageLogLifecycle() {
        Equipment equipment = createEquipment("Motor pump");

        var created = usageLogService.addUsageLog(new UsageLogCreationRequest(
                equipment.getId(),
                LocalDate.of(2026, 7, 23),
                new BigDecimal("6.50"),
                TestPeople.idOf(personDirectory, "Wilfried Y."),
                "Plowing field section B"
        ));

        assertEquals(equipment.getId(), created.equipmentId());
        assertEquals(new BigDecimal("6.50"), created.hoursUsed());
        assertEquals(1, usageLogService.obtainAllUsageLogs(PageRequest.of(0, 10)).getTotalElements());

        var updated = usageLogService.updateUsageLog(created.id(),
                new UsageLogUpdateRequest(null, null, new BigDecimal("8.25"), null, null));

        assertEquals(new BigDecimal("8.25"), updated.hoursUsed());

        usageLogService.deleteUsageLog(created.id());
        entityManager.flush();
        entityManager.clear();

        assertThrows(UsageLogNotFoundException.class,
                () -> usageLogService.obtainUsageLogById(created.id()));
    }

    @Test
    void fuelLogLifecycle() {
        Equipment equipment = createEquipment("Peeler");

        var created = fuelLogService.addFuelLog(new FuelLogCreationRequest(
                equipment.getId(),
                LocalDate.of(2026, 7, 22),
                new BigDecimal("25.00"),
                new BigDecimal("37.50")
        ));

        assertEquals(equipment.getId(), created.equipmentId());
        assertEquals(new BigDecimal("37.50"), created.cost());
        assertEquals(1, fuelLogService.obtainAllFuelLogs(PageRequest.of(0, 10)).getTotalElements());

        var updated = fuelLogService.updateFuelLog(created.id(),
                new FuelLogUpdateRequest(null, null, new BigDecimal("30.00"), null));

        assertEquals(new BigDecimal("30.00"), updated.liters());

        fuelLogService.deleteFuelLog(created.id());
        entityManager.flush();
        entityManager.clear();

        assertThrows(FuelLogNotFoundException.class, () -> fuelLogService.obtainFuelLogById(created.id()));
    }

    @Test
    void repairLogLifecycle() {
        Equipment equipment = createEquipment("Dryer");

        var created = repairLogService.addRepairLog(new RepairLogCreationRequest(
                equipment.getId(),
                LocalDate.of(2026, 7, 22),
                "Heating element malfunction",
                new BigDecimal("14.00"),
                new BigDecimal("150.00"),
                TestPeople.idOf(personDirectory, "Maintenance Team A")
        ));

        assertEquals(equipment.getId(), created.equipmentId());
        assertEquals("Heating element malfunction", created.issue());
        assertEquals(1, repairLogService.obtainAllRepairLogs(PageRequest.of(0, 10)).getTotalElements());

        var updated = repairLogService.updateRepairLog(created.id(),
                new RepairLogUpdateRequest(null, null, "Blade drive belt snapped", null, null, null));

        assertEquals("Blade drive belt snapped", updated.issue());

        repairLogService.deleteRepairLog(created.id());
        entityManager.flush();
        entityManager.clear();

        assertThrows(RepairLogNotFoundException.class,
                () -> repairLogService.obtainRepairLogById(created.id()));
    }

    @Test
    void logsReferenceAPersonOfTheDirectoryAndNeverCreateOne() {
        Equipment equipment = createEquipment("Sprayer");
        Long issa = TestPeople.idOf(personDirectory, "Issa K.");

        var schedule = maintenanceScheduleService.addMaintenanceSchedule(new MaintenanceScheduleCreationRequest(
                equipment.getId(), MaintenanceType.PREVENTIVE, null,
                LocalDate.of(2026, 7, 10), LocalDate.of(2026, 8, 5), issa));
        var usage = usageLogService.addUsageLog(new UsageLogCreationRequest(
                equipment.getId(), LocalDate.of(2026, 7, 23), new BigDecimal("2.00"), issa, null));
        var repair = repairLogService.addRepairLog(new RepairLogCreationRequest(
                equipment.getId(), LocalDate.of(2026, 7, 22), "Nozzle clogged", null, null, issa));
        entityManager.flush();
        entityManager.clear();

        assertEquals(issa, entityManager.find(MaintenanceSchedule.class, schedule.id()).getOperatorPersonId());
        assertEquals(issa, entityManager.find(UsageLog.class, usage.id()).getOperatorPersonId());
        assertEquals(issa, entityManager.find(RepairLog.class, repair.id()).getTechnicianPersonId());
        assertEquals("Issa K.", usageLogService.obtainUsageLogById(usage.id()).operator());
        assertEquals(issa, usageLogService.obtainUsageLogById(usage.id()).operatorPersonId());

        assertThrows(BadRequestException.class, () -> usageLogService.addUsageLog(new UsageLogCreationRequest(
                equipment.getId(), LocalDate.of(2026, 7, 24), new BigDecimal("1.00"), 999_999L, null)));
        assertEquals(1L, personRepository.count());
    }

    private Equipment createEquipment(String name) {
        Equipment equipment = new Equipment();
        equipment.setName(name);
        equipment.setCode("EQ-TEST-" + name);
        equipment.setCategory(Category.PROCESSING_MACHINERY);
        equipment.setStage(Stage.PROCESSING);
        equipment.setStatus(Status.OPERATIONAL);
        return equipmentRepository.save(equipment);
    }
}
