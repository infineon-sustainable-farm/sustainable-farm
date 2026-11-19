package com.infineonbit.sustainablefarm.modules.producttransformation.equipment;

import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.EquipmentType;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.model.BatchStatus;
import com.infineonbit.sustainablefarm.modules.producttransformation.block.ProductTransformationBlocks;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.machinery.api.Stage;
import com.infineonbit.sustainablefarm.modules.machinery.service.EquipmentService;
import com.infineonbit.sustainablefarm.modules.plants.VarietyCatalogTestData;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyReferenceRepository;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.model.Batch;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.repository.BatchRepository;
import com.infineonbit.sustainablefarm.modules.producttransformation.dryingrun.model.DryingRun;
import com.infineonbit.sustainablefarm.modules.producttransformation.dryingrun.repository.DryingRunRepository;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.Equipment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * The product transformation tables reference the machinery {@code equipment} register with
 * real foreign keys ({@code CrossModuleForeignKey}): the attributes go with the equipment, a
 * machine still used by process records cannot be deleted (as before the merge), and an
 * unknown equipment id is refused.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Foreign keys from product transformation to the equipment register")
class EquipmentRegisterForeignKeyTest {

    @Autowired
    private com.infineonbit.sustainablefarm.modules.producttransformation.equipment.service.EquipmentService
            productTransformationEquipment;

    @Autowired
    private EquipmentService machineryEquipment;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private ProductTransformationBlocks blocks;

    @Autowired
    private DryingRunRepository dryingRunRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private VarietyReferenceRepository varietyReferenceRepository;

    private Equipment createDryer(String name) {
        return ProcessingMachines.register(machineryEquipment, productTransformationEquipment,
                "FK dryer " + name, Stage.DRYING, EquipmentType.SOLAR_DRYER, "100.00", "0.5000");
    }

    private DryingRun dryingRun(String runId, Long equipmentId) {
        VarietyCatalogTestData.ensureDefaults(varietyReferenceRepository);
        Batch batch = new Batch();
        batch.setBatchId("B-" + runId);
        batch.setHarvestDate(LocalDate.now());
        batch.setVarietyKey("KENT");
        batch.setHarvestQuantityKg(new BigDecimal("500.00"));
        batch.setCurrentStatus(BatchStatus.CREATED);
        batch.setFarmBlockId(blocks.resolve("F-FK", "BL-FK"));
        batchRepository.saveAndFlush(batch);

        DryingRun run = new DryingRun();
        run.setRunId(runId);
        run.setBatch(batch);
        run.setEquipmentId(equipmentId);
        run.setDurationHours(new BigDecimal("6.50"));
        run.setTargetTemperatureC(new BigDecimal("55.00"));
        run.setActualTemperatureC(new BigDecimal("54.50"));
        run.setStartMoisturePct(new BigDecimal("78.00"));
        run.setEndMoisturePct(new BigDecimal("12.00"));
        run.setEnergyUsageKwh(new BigDecimal("118.40"));
        run.setStartTime(LocalDateTime.now().minusHours(7));
        run.setEndTime(LocalDateTime.now());
        return run;
    }

    @Test
    @DisplayName("Deleting an unused equipment deletes its attributes")
    void deletingAnUnusedEquipmentCascadesToTheSpec() {
        Equipment dryer = createDryer("cascade");

        machineryEquipment.deleteEquipment(dryer.getId());

        assertThat(jdbc.queryForObject(
                "select count(*) from pt_equipment_spec where equipment_id = ?", Long.class, dryer.getId()))
                .isZero();
    }

    @Test
    @DisplayName("An equipment still used by a drying run cannot be deleted")
    void deletingAnEquipmentStillUsedIsRefused() {
        Equipment dryer = createDryer("used");
        dryingRunRepository.saveAndFlush(dryingRun("DR-FK-USED", dryer.getId()));

        assertThatThrownBy(() -> machineryEquipment.deleteEquipment(dryer.getId()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("A drying run cannot reference an equipment id absent from the register")
    void anUnknownEquipmentIdIsRefused() {
        assertThatThrownBy(() -> dryingRunRepository.saveAndFlush(dryingRun("DR-FK-UNKNOWN", 987654321L)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
