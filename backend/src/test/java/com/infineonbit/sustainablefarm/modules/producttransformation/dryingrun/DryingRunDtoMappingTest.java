package com.infineonbit.sustainablefarm.modules.producttransformation.dryingrun;

import com.infineonbit.sustainablefarm.modules.staff.api.TestPeople;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.model.Role;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.model.ActiveStatus;
import com.infineonbit.sustainablefarm.modules.machinery.api.Stage;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.ProcessingMachines;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.EquipmentType;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.model.BatchStatus;
import com.infineonbit.sustainablefarm.modules.farm.support.SiteTestBlocks;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import com.infineonbit.sustainablefarm.modules.plants.VarietyCatalogTestData;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyReferenceRepository;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.model.Batch;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.repository.BatchRepository;
import com.infineonbit.sustainablefarm.modules.producttransformation.dashboard.service.DtoMapper;
import com.infineonbit.sustainablefarm.modules.producttransformation.dryingrun.dto.response.DryingRunResponse;
import com.infineonbit.sustainablefarm.modules.producttransformation.dryingrun.model.DryingRun;
import com.infineonbit.sustainablefarm.modules.producttransformation.dryingrun.repository.DryingRunRepository;
import com.infineonbit.sustainablefarm.modules.producttransformation.dryingrun.service.DryingRunService;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.Equipment;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.service.EquipmentService;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.model.Operator;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.repository.OperatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Regression test for the HTTP 500 on {@code GET /api/product-transformation/drying-runs}.
 *
 * <p>{@code DryingRunResponse.energyEfficiencyKwhPerKg} is derived from
 * {@code batch.harvestQuantityKg}, so the batch association must be fetched by
 * the repository. With {@code spring.jpa.open-in-view=false} the entities are
 * detached when the service transaction ends: if the adapter only relies on
 * lazy loading, the DTO mapping throws {@code LazyInitializationException} and
 * the endpoint answers 500. This test maps detached entities on purpose, which
 * is exactly the production request boundary.
 */
@SpringBootTest
@ActiveProfiles("test")
class DryingRunDtoMappingTest {

    private static final String BATCH_ID = "B-DRY-DTO-001";
    private static final String EQUIPMENT_NAME = "DTO dryer";
    private static final String OPERATOR_ID = "OP-DRY-DTO-001";
    private static final String RUN_ID = "DR-DRY-DTO-001";

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private SiteTestBlocks siteTestBlocks;

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private com.infineonbit.sustainablefarm.modules.machinery.service.EquipmentService machineryEquipment;

    @Autowired
    private OperatorRepository operatorRepository;

    @Autowired
    private PersonDirectory personDirectory;

    @Autowired
    private DryingRunRepository dryingRunRepository;

    @Autowired
    private DryingRunService dryingRunService;

    @Autowired
    private DtoMapper dtoMapper;

    @Autowired
    private VarietyReferenceRepository varietyReferenceRepository;

    @BeforeEach
    void seedDryingRun() {
        VarietyCatalogTestData.ensureDefaults(varietyReferenceRepository);
        if (dryingRunRepository.existsByRunId(RUN_ID)) {
            return;
        }

        Batch batch = new Batch();
        batch.setBatchId(BATCH_ID);
        batch.setHarvestDate(LocalDate.now());
        batch.setVarietyKey("KENT");
        batch.setHarvestQuantityKg(new BigDecimal("500.00"));
        batch.setCurrentStatus(BatchStatus.CREATED);
        batch.setFarmBlockId(siteTestBlocks.ensure("BL-DTO").id());
        batchRepository.saveAndFlush(batch);

        Equipment registered = equipmentService.getAllEquipment().stream()
                .filter(machine -> EQUIPMENT_NAME.equals(machine.getEquipmentName()))
                .findFirst()
                .orElseGet(() -> ProcessingMachines.register(machineryEquipment, equipmentService,
                        EQUIPMENT_NAME, Stage.DRYING, EquipmentType.DRYING, "250.00", "0.2500"));

        Operator operator = new Operator();
        operator.setOperatorId(OPERATOR_ID);
        operator.setPersonId(TestPeople.idOf(personDirectory, "DTO operator"));
        operator.setRole(Role.DRYER);
        operator.setActiveStatus(ActiveStatus.ACTIVE);
        operator.setHireDate(LocalDate.now());
        operatorRepository.saveAndFlush(operator);

        DryingRun run = new DryingRun();
        run.setRunId(RUN_ID);
        run.setBatch(batch);
        run.setEquipmentId(registered.getId());
        run.setOperator(operator);
        run.setDurationHours(new BigDecimal("6.50"));
        run.setTargetTemperatureC(new BigDecimal("55.00"));
        run.setActualTemperatureC(new BigDecimal("54.50"));
        run.setStartMoisturePct(new BigDecimal("78.00"));
        run.setEndMoisturePct(new BigDecimal("12.00"));
        run.setEnergyUsageKwh(new BigDecimal("118.40"));
        run.setStartTime(LocalDateTime.now().minusHours(7));
        run.setEndTime(LocalDateTime.now());
        dryingRunRepository.saveAndFlush(run);
    }

    @Test
    @DisplayName("GET /api/product-transformation/drying-runs/{id}: the response maps a detached run")
    void detailedResponseMapsDetachedDryingRun() {
        DryingRun run = dryingRunService.getDryingRunById(RUN_ID);

        assertThatCode(() -> dtoMapper.toResponse(run)).doesNotThrowAnyException();

        DryingRunResponse response = dtoMapper.toResponse(run);
        assertThat(response.getBatchId()).isEqualTo(BATCH_ID);
        assertThat(response.getEquipmentId()).isEqualTo(equipmentService.getEquipmentByRegisterId(run.getEquipmentId())
                .getEquipmentId());
        assertThat(response.getEquipmentId()).startsWith("EQ-");
        assertThat(response.getOperatorId()).isEqualTo(OPERATOR_ID);
        // 118.40 kWh / 500 kg, computed from the batch association.
        assertThat(response.getEnergyEfficiencyKwhPerKg()).isCloseTo(0.2368,
                org.assertj.core.data.Offset.offset(0.0001));
        // (78 - 12) / 78 * 100, rounded to two decimals by the mapper.
        assertThat(response.getMoistureReductionPct()).isCloseTo(84.62,
                org.assertj.core.data.Offset.offset(0.0001));
        assertThat(response.getWithinTargetRange()).isTrue();
    }

    @Test
    @DisplayName("GET /api/product-transformation/drying-runs: the list response maps detached runs")
    void listResponseMapsDetachedDryingRuns() {
        List<DryingRun> runs = dryingRunService.getAllDryingRuns();

        DryingRunResponse response = runs.stream()
                .filter(run -> RUN_ID.equals(run.getRunId()))
                .findFirst()
                .map(dtoMapper::toResponse)
                .orElseThrow(() -> new AssertionError("Seeded drying run missing from the list response"));

        assertThat(response.getBatchId()).isEqualTo(BATCH_ID);
        assertThat(response.getEnergyEfficiencyKwhPerKg()).isCloseTo(0.2368,
                org.assertj.core.data.Offset.offset(0.0001));
    }
}
