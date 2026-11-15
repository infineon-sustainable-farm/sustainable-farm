package com.infineonbit.sustainablefarm.modules.producttransformation.rawintake;

import com.infineonbit.sustainablefarm.modules.producttransformation.batch.model.Batch;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.repository.BatchRepository;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent;
import com.infineonbit.sustainablefarm.modules.producttransformation.rawintake.model.RawIntake;
import com.infineonbit.sustainablefarm.modules.producttransformation.rawintake.repository.RawIntakeRepository;
import com.infineonbit.sustainablefarm.modules.producttransformation.rawintake.service.RawIntakeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class RawIntakeIntegrationTest {

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private RawIntakeRepository rawIntakeRepository;

    @Autowired
    private RawIntakeService rawIntakeService;

    @Test
    public void testBusinessJourneyBatchThenRawIntakeThenInitialize() {
        // 1. Create Batch (precondition for RawIntake per contract)
        Batch batch = new Batch();
        batch.setBatchId("B-TEST-001");
        batch.setHarvestDate(LocalDate.now());
        batch.setMangoVariety(HarvestEvent.MangoVariety.KENT);
        batch.setHarvestQuantityKg(new BigDecimal("100.00"));
        batch.setCurrentStatus(Batch.BatchStatus.CREATED);
        batch.setFarmId("F-TEST");
        batch.setBlockId("BL-TEST");
        Batch savedBatch = batchRepository.save(batch);
        assertThat(savedBatch.getBatchId()).isEqualTo("B-TEST-001");

        // 2. Create RawIntake referencing existing Batch
        RawIntake intake = new RawIntake();
        intake.setIntakeId("RI-TEST-001");
        intake.setBatch(savedBatch);
        intake.setSourceFarm("TestFarm");
        intake.setSourceBlock("BL-01");
        intake.setIntakeDate(LocalDate.now());
        intake.setReceivedQuantityKg(new BigDecimal("95.00"));
        intake.setReceivedVariety(HarvestEvent.MangoVariety.KENT);
        intake.setReceivedGrade(HarvestEvent.QualityGrade.A);
        intake.setIntakeOperator("op-test");
        RawIntake savedIntake = rawIntakeService.createRawIntake(intake);
        assertThat(savedIntake.getIntakeId()).isEqualTo("RI-TEST-001");
        assertThat(savedIntake.getBatch()).isNotNull();
        assertThat(savedIntake.getBatch().getBatchId()).isEqualTo("B-TEST-001");

        // 3. Initialize batch from intake (business rule)
        RawIntake initialized = rawIntakeService.initializeBatchFromIntake("RI-TEST-001");
        assertThat(initialized.getBatch()).isNotNull();

        // 4. Verify persistence link
        RawIntake readIntake = rawIntakeRepository.findById("RI-TEST-001").orElseThrow();
        assertThat(readIntake.getBatch().getBatchId()).isEqualTo("B-TEST-001");

        Batch readBatch = batchRepository.findById("B-TEST-001").orElseThrow();
        assertThat(readBatch.getCurrentStatus()).isEqualTo(Batch.BatchStatus.INTAKE);
    }
}
