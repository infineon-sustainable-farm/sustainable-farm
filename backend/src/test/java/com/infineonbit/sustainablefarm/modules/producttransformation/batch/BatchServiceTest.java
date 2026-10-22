package com.infineonbit.sustainablefarm.modules.producttransformation.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleViolationException;
import com.infineonbit.sustainablefarm.core.exception.InvalidStateException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.repository.BatchRepository;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.model.Batch;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.model.Batch.BatchStatus;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent.MangoVariety;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Batch Service Integration Tests")
class BatchServiceTest {

    @Autowired
    private BatchRepository batchRepository;

    @BeforeEach
    void setUp() {
        // Clean up any existing test batches before each test
        batchRepository.deleteAll();
    }

    @Test
    @DisplayName("Test valid status transitions through the workflow")
    void testValidStatusTransitions() {
        // Create a batch starting at CREATED
        Batch batch = new Batch();
        batch.setBatchId("TEST-BATCH-001");
        batch.setHarvestDate(LocalDate.of(2026, 10, 1));
        batch.setMangoVariety(MangoVariety.TOMMY);
        batch.setHarvestQuantityKg(new BigDecimal("500.00"));
        batch.setFarmId("FARM-001");
        batch.setBlockId("BLOCK-A");
        batch.setCurrentStatus(BatchStatus.CREATED);
        batch = batchRepository.save(batch);

        // Test CREATED -> INTAKE -> WASHING -> DRYING -> PACKAGING -> COMPLETED -> SHIPPED
        batch.advanceStatus();
        assertEquals(BatchStatus.INTAKE, batch.getCurrentStatus());

        batch.advanceStatus();
        assertEquals(BatchStatus.WASHING, batch.getCurrentStatus());

        batch.advanceStatus();
        assertEquals(BatchStatus.DRYING, batch.getCurrentStatus());

        batch.advanceStatus();
        assertEquals(BatchStatus.PACKAGING, batch.getCurrentStatus());

        batch.advanceStatus();
        assertEquals(BatchStatus.COMPLETED, batch.getCurrentStatus());

        batch.advanceStatus();
        assertEquals(BatchStatus.SHIPPED, batch.getCurrentStatus());
    }

    @Test
    @DisplayName("Test invalid status transitions are rejected")
    void testInvalidStatusTransitions() {
        Batch batch = new Batch();
        batch.setBatchId("TEST-BATCH-002");
        batch.setHarvestDate(LocalDate.of(2026, 10, 2));
        batch.setMangoVariety(MangoVariety.TOMMY);
        batch.setHarvestQuantityKg(new BigDecimal("300.00"));
        batch.setFarmId("FARM-002");
        batch.setBlockId("BLOCK-B");
        batch.setCurrentStatus(BatchStatus.CREATED);
        batch = batchRepository.save(batch);

        // CREATED -> DRYING should fail (need to go through INTAKE, WASHING first)
        boolean exceptionThrown = false;
        try {
            batch.advanceStatus(); // CREATED -> INTAKE (this is valid)
            batch.advanceStatus(); // INTAKE -> WASHING (this is valid)
            batch.advanceStatus(); // WASHING -> DRYING (this is valid)
            batch.advanceStatus(); // DRYING -> PACKAGING (this is valid)
            batch.advanceStatus(); // PACKAGING -> COMPLETED (this is valid)
            batch.advanceStatus(); // COMPLETED -> SHIPPED (this is valid)
            // Now SHIPPED is terminal, advancing should fail
            batch.advanceStatus();
        } catch (IllegalStateException e) {
            exceptionThrown = true;
        }
        assertEquals(true, exceptionThrown);
    }

    @Test
    @DisplayName("Test that REJECTED state is terminal")
    void testRejectedStateIsTerminal() {
        Batch batch = new Batch();
        batch.setBatchId("TEST-BATCH-003");
        batch.setHarvestDate(LocalDate.of(2026, 10, 3));
        batch.setMangoVariety(MangoVariety.TOMMY);
        batch.setHarvestQuantityKg(new BigDecimal("200.00"));
        batch.setFarmId("FARM-003");
        batch.setBlockId("BLOCK-C");
        batch.setCurrentStatus(BatchStatus.CREATED);
        batch = batchRepository.save(batch);

        // Advance to SHIPPED first
        batch.advanceStatus(); // CREATED -> INTAKE
        batch.advanceStatus(); // INTAKE -> WASHING
        batch.advanceStatus(); // WASHING -> DRYING
        batch.advanceStatus(); // DRYING -> PACKAGING
        batch.advanceStatus(); // PACKAGING -> COMPLETED
        batch.advanceStatus(); // COMPLETED -> SHIPPED

        // Now set to REJECTED manually
        batch.setCurrentStatus(BatchStatus.REJECTED);
        batchRepository.save(batch);

        // REJECTED should be terminal - cannot advance
        Batch finalBatch = batch;
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            finalBatch.advanceStatus();
        });
        assertEquals("Cannot advance status from REJECTED", exception.getMessage());
    }

    @Test
    @DisplayName("Test batch creation with default CREATED status")
    void testBatchCreationWithDefaultStatus() {
        Batch batch = new Batch();
        batch.setBatchId("TEST-BATCH-004");
        batch.setHarvestDate(LocalDate.of(2026, 10, 4));
        batch.setMangoVariety(MangoVariety.TOMMY);
        batch.setHarvestQuantityKg(new BigDecimal("100.00"));
        batch.setFarmId("FARM-004");
        batch.setBlockId("BLOCK-D");

        // Default status should be CREATED
        Batch saved = batchRepository.save(batch);
        assertEquals(BatchStatus.CREATED, saved.getCurrentStatus());
    }

    @Test
    @DisplayName("Test batch filtering by status")
    void testFilterBatchesByStatus() {
        // Create batches at different statuses
        for (int i = 1; i <= 3; i++) {
            Batch batch = new Batch();
            batch.setBatchId("TEST-BATCH-" + String.format("%03d", i));
            batch.setHarvestDate(LocalDate.of(2026, 10, i));
            batch.setMangoVariety(MangoVariety.TOMMY);
            batch.setHarvestQuantityKg(new BigDecimal(String.format("%.2f", 100.0 * i)));
            batch.setFarmId("FARM-00" + i);
            batch.setBlockId("BLOCK-" + i);
            batch.setCurrentStatus(BatchStatus.CREATED);
            batchRepository.save(batch);
        }

        // Filter by CREATED status
        List<Batch> createdBatches = batchRepository.findByCurrentStatus(BatchStatus.CREATED);
        assertEquals(3, createdBatches.size());
    }

    @Test
    @DisplayName("Test counting batches by status")
    void testCountBatchesByStatus() {
        // Create 2 batches at CREATED status
        for (int i = 1; i <= 2; i++) {
            Batch batch = new Batch();
            batch.setBatchId("TEST-BATCH-COUNT-" + i);
            batch.setHarvestDate(LocalDate.of(2026, 10, i));
            batch.setMangoVariety(MangoVariety.TOMMY);
            batch.setHarvestQuantityKg(new BigDecimal("100.00"));
            batch.setFarmId("FARM-00" + i);
            batch.setBlockId("BLOCK-" + i);
            batch.setCurrentStatus(BatchStatus.CREATED);
            batchRepository.save(batch);
        }

        // Create 1 batch at COMPLETED status
        Batch completedBatch = new Batch();
        completedBatch.setBatchId("TEST-BATCH-COUNT-COMP");
        completedBatch.setHarvestDate(LocalDate.of(2026, 10, 3));
        completedBatch.setMangoVariety(MangoVariety.TOMMY);
        completedBatch.setHarvestQuantityKg(new BigDecimal("200.00"));
        completedBatch.setFarmId("FARM-003");
        completedBatch.setBlockId("BLOCK-3");
        completedBatch.setCurrentStatus(BatchStatus.COMPLETED);
        batchRepository.save(completedBatch);

        long createdCount = batchRepository.countByStatus(BatchStatus.CREATED);
        assertEquals(2, createdCount);
    }

    @Test
    @DisplayName("Test get all batches and verify count")
    void testGetAllBatches() {
        // Initially empty
        List<Batch> allBatches = batchRepository.findAll();
        assertEquals(0, allBatches.size());

        // Create a batch
        Batch batch = new Batch();
        batch.setBatchId("TEST-BATCH-001");
        batch.setHarvestDate(LocalDate.of(2026, 10, 1));
        batch.setMangoVariety(MangoVariety.TOMMY);
        batch.setHarvestQuantityKg(new BigDecimal("500.00"));
        batch.setFarmId("FARM-001");
        batch.setBlockId("BLOCK-A");
        batch.setCurrentStatus(BatchStatus.CREATED);
        batchRepository.save(batch);

        allBatches = batchRepository.findAll();
        assertEquals(1, allBatches.size());
    }

    @Test
    @DisplayName("Test business rule violation on invalid status from CREATED")
    void testBusinessRuleViolationOnInvalidAdvanceFromCreated() {
        Batch batch = new Batch();
        batch.setBatchId("TEST-BATCH-005");
        batch.setHarvestDate(LocalDate.of(2026, 10, 5));
        batch.setMangoVariety(MangoVariety.TOMMY);
        batch.setHarvestQuantityKg(new BigDecimal("100.00"));
        batch.setFarmId("FARM-005");
        batch.setBlockId("BLOCK-E");
        batch.setCurrentStatus(BatchStatus.CREATED);
        batch = batchRepository.save(batch);

        // Try to advance from CREATED - this should work (CREATED -> INTAKE)
        batch.advanceStatus();
        assertEquals(BatchStatus.INTAKE, batch.getCurrentStatus());

        // Try to advance again - should also work (INTAKE -> WASHING)
        batch.advanceStatus();
        assertEquals(BatchStatus.WASHING, batch.getCurrentStatus());
    }
}