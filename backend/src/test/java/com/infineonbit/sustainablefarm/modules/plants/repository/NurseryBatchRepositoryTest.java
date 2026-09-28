package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryBatch;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryOrigin;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
public class NurseryBatchRepositoryTest {

    @Autowired
    private NurseryBatchRepository nurseryBatchRepository;

    private NurseryBatch batch(Integer farmId, String batchCode, LocalDate startedOn) {
        return nurseryBatchRepository.save(new NurseryBatch(null, farmId, batchCode, "Keitt", NurseryOrigin.IN_HOUSE,
                null, null, startedOn, 150, startedOn.plusMonths(6), null, "user_entry", null));
    }

    private static List<Long> ids(List<NurseryBatch> batches) {
        return batches.stream().map(NurseryBatch::getId).toList();
    }

    @Test
    void findByFarmAndCode_shouldMatchOnlyTheBatchesWithoutFarm_whenTheFarmIsNull() {
        // Arrange
        NurseryBatch withoutFarm = batch(null, "P1", LocalDate.of(2026, 3, 2));
        batch(1, "P1", LocalDate.of(2026, 3, 2));
        batch(null, "P2", LocalDate.of(2026, 3, 2));
        // Act & Assert: a missing farm is a farm of its own, never every farm
        assertEquals(List.of(withoutFarm.getId()), ids(nurseryBatchRepository.findByFarmAndCode(null, "P1")));
        assertEquals(List.of(), ids(nurseryBatchRepository.findByFarmAndCode(null, "P9")));
    }

    @Test
    void findByFarmAndCode_shouldMatchOnlyThatFarm_whenAFarmIsGiven() {
        // Arrange
        batch(null, "P1", LocalDate.of(2026, 3, 2));
        NurseryBatch ofFarm1 = batch(1, "P1", LocalDate.of(2026, 3, 2));
        batch(2, "P1", LocalDate.of(2026, 3, 2));
        // Act & Assert
        assertEquals(List.of(ofFarm1.getId()), ids(nurseryBatchRepository.findByFarmAndCode(1, "P1")));
        assertEquals(List.of(), ids(nurseryBatchRepository.findByFarmAndCode(3, "P1")));
    }

    @Test
    void findByOptionalFarm_shouldReturnEveryFarm_byStartDateThenIdentifier() {
        // Arrange: saved out of date order, two on the same day
        NurseryBatch late = batch(null, "P2", LocalDate.of(2026, 7, 1));
        NurseryBatch early = batch(1, "P1", LocalDate.of(2026, 3, 2));
        NurseryBatch sameDay = batch(null, "P3", LocalDate.of(2026, 3, 2));
        // Act & Assert
        assertEquals(List.of(early.getId(), sameDay.getId(), late.getId()),
                ids(nurseryBatchRepository.findByOptionalFarm(null)));
    }

    @Test
    void findByOptionalFarm_shouldKeepOnlyThatFarm_whenAFarmIsGiven() {
        // Arrange
        batch(null, "P1", LocalDate.of(2026, 3, 2));
        NurseryBatch ofFarm1 = batch(1, "P1", LocalDate.of(2026, 3, 2));
        batch(2, "P1", LocalDate.of(2026, 3, 2));
        // Act & Assert
        assertEquals(List.of(ofFarm1.getId()), ids(nurseryBatchRepository.findByOptionalFarm(1)));
        assertEquals(List.of(), ids(nurseryBatchRepository.findByOptionalFarm(3)));
    }
}
