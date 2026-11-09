package com.infineonbit.sustainablefarm.modules.producttransformation.dryingrun;

import com.infineonbit.sustainablefarm.modules.producttransformation.batch.model.Batch;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.repository.BatchRepository;
import com.infineonbit.sustainablefarm.modules.producttransformation.dashboard.service.DtoMapper;
import com.infineonbit.sustainablefarm.modules.producttransformation.dryingrun.dto.response.DryingRunResponse;
import com.infineonbit.sustainablefarm.modules.producttransformation.dryingrun.model.DryingRun;
import com.infineonbit.sustainablefarm.modules.producttransformation.dryingrun.repository.DryingRunRepository;
import com.infineonbit.sustainablefarm.modules.producttransformation.dryingrun.service.DryingRunService;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.Equipment;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.repository.EquipmentRepository;
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
 * Regression test for the HTTP 500 on {@code GET /api/drying-runs}.
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
    private static final String EQUIPMENT_ID = "EQ-DRY-DTO-001";
    private static final String OPERATOR_ID = "OP-DRY-DTO-001";
    private static final String RUN_ID = "DR-DRY-DTO-001";

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private OperatorRepository operatorRepository;

    @Autowired
    private DryingRunRepository dryingRunRepository;

    @Autowired
    private DryingRunService dryingRunService;

    @Autowired
    private DtoMapper dtoMapper;

    @BeforeEach
    void seedDryingRun() {
        if (dryingRunRepository.existsById(RUN_ID)) {
            return;
        }

        Batch batch = new Batch();
        batch.setBatchId(BATCH_ID);
        batch.setHarvestDate(LocalDate.now());
        batch.setMangoVariety(HarvestEvent.MangoVariety.KENT);
        batch.setHarvestQuantityKg(new BigDecimal("500.00"));
        batch.setCurrentStatus(Batch.BatchStatus.CREATED);
        batch.setFarmId("F-DTO");
        batch.setBlockId("BL-DTO");
        batchRepository.saveAndFlush(batch);

        Equipment equipment = new Equipment();
        equipment.setEquipmentId(EQUIPMENT_ID);
        equipment.setEquipmentName("DTO dryer");
        equipment.setEquipmentType(Equipment.EquipmentType.DRYING);
        equipment.setCapacityKgPerHour(new BigDecimal("250.00"));
        equipment.setEnergyConsumptionKwhPerKg(new BigDecimal("0.2500"));
        equipment.setMaintenanceStatus(Equipment.MaintenanceStatus.ACTIVE);
        equipmentRepository.saveAndFlush(equipment);

        Operator operator = new Operator();
        operator.setOperatorId(OPERATOR_ID);
        operator.setOperatorName("DTO operator");
        operator.setRole(Operator.Role.DRYER);
        operator.setActiveStatus(Operator.ActiveStatus.ACTIVE);
        operator.setHireDate(LocalDate.now());
        operatorRepository.saveAndFlush(operator);

        DryingRun run = new DryingRun();
        run.setRunId(RUN_ID);
        run.setBatch(batch);
        run.setEquipment(equipment);
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
    @DisplayName("GET /api/drying-runs/{id}: the response maps a detached run")
    void detailedResponseMapsDetachedDryingRun() {
        DryingRun run = dryingRunService.getDryingRunById(RUN_ID);

        assertThatCode(() -> dtoMapper.toResponse(run)).doesNotThrowAnyException();

        DryingRunResponse response = dtoMapper.toResponse(run);
        assertThat(response.getBatchId()).isEqualTo(BATCH_ID);
        assertThat(response.getEquipmentId()).isEqualTo(EQUIPMENT_ID);
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
    @DisplayName("GET /api/drying-runs: the list response maps detached runs")
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
