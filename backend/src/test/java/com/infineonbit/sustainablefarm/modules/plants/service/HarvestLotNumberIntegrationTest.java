package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Request.HarvestRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PlantingRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HarvestResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The lot numbers of the harvests, through the real service and the database:
 * the next number is read from the lots already stored.
 *
 * <p>The race case sends its requests together, from separate threads released
 * at the same instant, and runs {@value #TRIALS} times on new blocks, as in
 * {@link PlantingConcurrencyIntegrationTest}: H2 does not reproduce a race on
 * every trial. The writes go through {@link ConcurrentPlantingRetry}, as the
 * controller sends them. The blocks starting with LN and LR are used by no
 * other test.
 */
@SpringBootTest
@ActiveProfiles("test")
class HarvestLotNumberIntegrationTest {

    private static final int TRIALS = 10;
    private static final LocalDate PLANTED_ON = LocalDate.of(2023, 9, 15);
    private static final LocalDate HARVESTED_ON = LocalDate.of(2026, 6, 10);

    @Autowired
    private PlantingService plantingService;

    @Autowired
    private HarvestService harvestService;

    @Autowired
    private ConcurrentPlantingRetry concurrentPlantingRetry;

    private HarvestResponse harvest(String blockCode, String varietyName, LocalDate harvestDate) {
        return harvestService.recordHarvest(new HarvestRequest(null, blockCode, varietyName, harvestDate, 50.0));
    }

    /**
     * Runs the calls together and returns, for each one in order, its result
     * or the exception it threw.
     */
    private static List<Object> together(List<Callable<Object>> calls) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(calls.size());
        CountDownLatch ready = new CountDownLatch(calls.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Object>> futures = new ArrayList<>();
        try {
            for (Callable<Object> call : calls) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        return call.call();
                    } catch (RuntimeException refused) {
                        return refused;
                    }
                }));
            }
            ready.await();
            start.countDown();
            List<Object> results = new ArrayList<>();
            for (Future<Object> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    /** A harvest sent as the controller sends it, run once more after a lost race. */
    private Callable<Object> harvestAsTheController(String blockCode, String varietyName) {
        HarvestRequest request = new HarvestRequest(null, blockCode, varietyName, HARVESTED_ON, 50.0);
        return () -> concurrentPlantingRetry.runRetryingOnce(blockCode, () -> harvestService.recordHarvest(request));
    }

    @Test
    void recordHarvest_shouldNumberTheLotsOfABlockAndYearInTheOrderTheyAreRecorded() {
        // Arrange: Keitt and Kent planted on block LN1
        plantingService.recordPlanting(new PlantingRequest(null, "LN1", "Keitt", PLANTED_ON, 40));
        plantingService.recordPlanting(new PlantingRequest(null, "LN1", "Kent", PLANTED_ON, 20));
        // Act: two pickings of 2026 on two varieties, the second one dated earlier, then one of 2025 recorded late
        HarvestResponse first = harvest("LN1", "Keitt", HARVESTED_ON);
        HarvestResponse second = harvest(" ln1 ", "Kent", HARVESTED_ON.minusDays(5));
        HarvestResponse late = harvest("LN1", "Keitt", LocalDate.of(2025, 12, 30));
        // Assert: one sequence per block and year, whatever the variety, in the order of recording
        assertEquals("LOT-2026-LN1-001", first.lotNumber());
        assertEquals("LOT-2026-LN1-002", second.lotNumber());
        assertEquals("LOT-2025-LN1-001", late.lotNumber());
        // Assert: the number is stored, not made up when the harvest is read
        assertEquals("LOT-2026-LN1-002", harvestService.getHarvestById(second.id()).lotNumber());
    }

    @Test
    void recordHarvest_shouldShareTheSequenceOfABlockBetweenFarms() {
        // Arrange: block LN2 on no farm and on farm 1
        plantingService.recordPlanting(new PlantingRequest(null, "LN2", "Keitt", PLANTED_ON, 40));
        plantingService.recordPlanting(new PlantingRequest(1, "LN2", "Keitt", PLANTED_ON, 30));
        // Act
        HarvestResponse noFarm = harvest("LN2", "Keitt", HARVESTED_ON);
        HarvestResponse farm1 = harvestService.recordHarvest(new HarvestRequest(1, "LN2", "Keitt", HARVESTED_ON, 20.0));
        // Assert: one lot number is never given twice, even on two farms
        assertEquals("LOT-2026-LN2-001", noFarm.lotNumber());
        assertEquals("LOT-2026-LN2-002", farm1.lotNumber());
        assertEquals(1, farm1.farmId());
    }

    @Test
    void twoHarvestsOfABlockSentTogether_shouldBothBeRecordedWithTheirOwnLot() throws Exception {
        for (int trial = 1; trial <= TRIALS; trial++) {
            // Arrange: two varieties of one block, so no lock on a variety row could keep them apart
            String blockCode = "LR" + trial;
            plantingService.recordPlanting(new PlantingRequest(null, blockCode, "Keitt", PLANTED_ON, 40));
            plantingService.recordPlanting(new PlantingRequest(null, blockCode, "Kent", PLANTED_ON, 20));
            // Act
            List<Object> results = together(List.of(
                    harvestAsTheController(blockCode, "Keitt"),
                    harvestAsTheController(blockCode, "Kent")));
            // Assert: both recorded, numbers 001 and 002 in either order
            List<String> lots = new ArrayList<>();
            for (Object result : results) {
                lots.add(result instanceof HarvestResponse harvest ? harvest.lotNumber() : String.valueOf(result));
            }
            assertEquals(List.of("LOT-2026-" + blockCode + "-001", "LOT-2026-" + blockCode + "-002"),
                    lots.stream().sorted().toList(), "trial " + trial);
        }
    }
}
