package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.NurseryBatchRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PlantingRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.TransplantRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.NurseryBatchResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.NurseryEventResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantingResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.GrowthCalendar;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import com.infineonbit.sustainablefarm.modules.plants.repository.GrowthCalendarRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.PopulationEventRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.PopulationEventRepository.TreeBalance;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guards the plantings against requests sent at the same time.
 *
 * <p>Each case sends its requests together, from separate threads released at
 * the same instant, and runs {@value #TRIALS} times on new blocks: H2 does not
 * reproduce a race on every trial, so a single trial could pass by chance. The
 * writes go through {@link ConcurrentPlantingRetry}, as the controllers send
 * them.
 */
@SpringBootTest
@ActiveProfiles("test")
class PlantingConcurrencyIntegrationTest {

    private static final int TRIALS = 10;

    @Autowired
    private PlantingService plantingService;

    @Autowired
    private NurseryBatchService nurseryBatchService;

    @Autowired
    private NurseryEventService nurseryEventService;

    @Autowired
    private ConcurrentPlantingRetry concurrentPlantingRetry;

    @Autowired
    private VarietyRepository varietyRepository;

    @Autowired
    private GrowthCalendarRepository growthCalendarRepository;

    @Autowired
    private PopulationEventRepository populationEventRepository;

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

    private static long count(List<Object> results, Class<?> type) {
        return results.stream().filter(type::isInstance).count();
    }

    private static List<String> messages(List<Object> results, Class<? extends Exception> type) {
        return results.stream().filter(type::isInstance).map(result -> ((Exception) result).getMessage()).toList();
    }

    private Callable<Object> plant(String blockCode, String varietyName, String plantingDate, int treeCount) {
        PlantingRequest request = new PlantingRequest(null, blockCode, varietyName,
                LocalDate.parse(plantingDate), treeCount);
        return () -> concurrentPlantingRetry.runRetryingOnce(blockCode,
                () -> plantingService.recordPlanting(request));
    }

    private Callable<Object> transplant(Long batchId, String blockCode) {
        TransplantRequest request = new TransplantRequest(LocalDate.of(2026, 6, 1), 50.0, blockCode);
        return () -> concurrentPlantingRetry.runRetryingOnce(blockCode,
                () -> nurseryEventService.recordTransplant(batchId, request));
    }

    /** A batch of 100 Kent plants, ready to be transplanted. */
    private Long startKentBatch(String batchCode) {
        return nurseryBatchService.createBatch(new NurseryBatchRequest(null, batchCode, "Kent", "IN_HOUSE", null,
                null, LocalDate.of(2025, 1, 1), 100.0, "READY_TO_TRANSPLANT", LocalDate.of(2026, 6, 1), null)).id();
    }

    /** Current number of trees of each variety row of the block, oldest row first. */
    private List<Integer> treeBalances(String blockCode) {
        List<Long> ids = varietyRepository.findByOptionalFilters(null, blockCode).stream()
                .map(Variety::getId)
                .toList();
        return populationEventRepository.findTreeBalances(ids).stream()
                .map(TreeBalance::getBalance)
                .map(Long::intValue)
                .toList();
    }

    private List<LocalDate> calendarDates(String blockCode) {
        return growthCalendarRepository.findByFarmAndBlock(null, blockCode).stream()
                .map(GrowthCalendar::getPlantingDate)
                .toList();
    }

    @Test
    void existingRowWithoutPlanting_shouldGetOnePlantingAndOneConflict() throws Exception {
        for (int trial = 1; trial <= TRIALS; trial++) {
            // Arrange: a variety row with no planting, like the Zalka 2025 row of the dev profile
            String blockCode = "CE" + trial;
            Variety keitt = new Variety();
            keitt.setName("Keitt");
            keitt.setBlockCode(blockCode);
            keitt.setTreeCount(200);
            keitt.setSource("Zalka_2025");
            varietyRepository.save(keitt);
            // Act
            List<Object> results = together(List.of(
                    plant(blockCode, "Keitt", "2023-09-24", 150),
                    plant(blockCode, "Keitt", "2023-09-24", 150)));
            // Assert: one planting on the existing row, the other request refused
            assertEquals(1, count(results, PlantingResponse.class), "trial " + trial);
            assertEquals(List.of("A planting of Keitt is already recorded on block " + blockCode),
                    messages(results, ConflictException.class), "trial " + trial);
            assertEquals(List.of(150), treeBalances(blockCode), "trial " + trial);
        }
    }

    @Test
    void olderPlantingsSentTogether_shouldKeepTheOldestCalendarDate() throws Exception {
        for (int trial = 1; trial <= TRIALS; trial++) {
            // Arrange: the block already has a calendar row, dated by a first planting
            String blockCode = "CD" + trial;
            plantingService.recordPlanting(new PlantingRequest(null, blockCode, "Palmer",
                    LocalDate.of(2024, 1, 1), 10));
            // Act: two older plantings of other varieties, sent together
            List<Object> results = together(List.of(
                    plant(blockCode, "Kent", "2021-03-01", 50),
                    plant(blockCode, "Keitt", "2022-06-01", 100)));
            // Assert: both recorded, and the calendar keeps the oldest date
            assertEquals(2, count(results, PlantingResponse.class), "trial " + trial);
            assertEquals(List.of(LocalDate.of(2021, 3, 1)), calendarDates(blockCode), "trial " + trial);
        }
    }

    @Test
    void sameTripleSentTogether_shouldKeepOneRowAndAnswerTheExisting409() throws Exception {
        for (int trial = 1; trial <= TRIALS; trial++) {
            // Arrange: the triple has no row yet
            String blockCode = "CT" + trial;
            // Act
            List<Object> results = together(List.of(
                    plant(blockCode, "Kent", "2021-03-01", 50),
                    plant(blockCode, "Kent", "2021-03-01", 50)));
            // Assert: the late request ends like a planting sent after the first one
            assertEquals(1, count(results, PlantingResponse.class), "trial " + trial);
            assertEquals(List.of("A planting of Kent is already recorded on block " + blockCode),
                    messages(results, ConflictException.class), "trial " + trial);
            assertEquals(List.of(50), treeBalances(blockCode), "trial " + trial);
        }
    }

    @Test
    void fivePlantingsOfTheSameTriple_shouldKeepOneRowAndOnePlanting() throws Exception {
        for (int trial = 1; trial <= TRIALS; trial++) {
            // Arrange
            String blockCode = "CF" + trial;
            List<Callable<Object>> plantings = Collections.nCopies(5, plant(blockCode, "Kent", "2021-03-01", 50));
            // Act
            List<Object> results = together(plantings);
            // Assert: one 201 and four 409, one row with one planting of 50 trees
            assertEquals(1, count(results, PlantingResponse.class), "trial " + trial);
            assertEquals(4, count(results, ConflictException.class), "trial " + trial);
            assertEquals(1, varietyRepository.findByOptionalFilters(null, blockCode).size(), "trial " + trial);
            assertEquals(List.of(50), treeBalances(blockCode), "trial " + trial);
        }
    }

    @Test
    void sameTripleInAnotherCase_shouldReuseTheRowOfTheFirstPlanting() throws Exception {
        for (int trial = 1; trial <= TRIALS; trial++) {
            // Arrange
            String blockCode = "CC" + trial;
            // Act: the lookup already reads "kent" as "Kent"
            List<Object> results = together(List.of(
                    plant(blockCode, "Kent", "2021-03-01", 50),
                    plant(blockCode, "kent", "2021-03-01", 50)));
            // Assert
            assertEquals(1, count(results, PlantingResponse.class), "trial " + trial);
            assertEquals(1, count(results, ConflictException.class), "trial " + trial);
            assertEquals(List.of(50), treeBalances(blockCode), "trial " + trial);
        }
    }

    @Test
    void twoVarietiesOnANewBlock_shouldShareOneCalendarRowWithTheOldestDate() throws Exception {
        for (int trial = 1; trial <= TRIALS; trial++) {
            // Arrange: the block has no calendar row yet
            String blockCode = "CN" + trial;
            // Act
            List<Object> results = together(List.of(
                    plant(blockCode, "Kent", "2021-03-01", 50),
                    plant(blockCode, "Keitt", "2023-09-24", 100)));
            // Assert: both plantings recorded, one calendar row dated by the older one
            assertEquals(2, count(results, PlantingResponse.class), "trial " + trial);
            assertEquals(List.of(50, 100), treeBalances(blockCode).stream().sorted().toList(), "trial " + trial);
            assertEquals(List.of(LocalDate.of(2021, 3, 1)), calendarDates(blockCode), "trial " + trial);
        }
    }

    @Test
    void transplantAndPlantingOfTheSameVariety_shouldRecordOnlyOneOfThem() throws Exception {
        for (int trial = 1; trial <= TRIALS; trial++) {
            // Arrange
            String blockCode = "CP" + trial;
            Long batchId = startKentBatch("CP-" + trial);
            // Act
            List<Object> results = together(List.of(
                    transplant(batchId, blockCode),
                    plant(blockCode, "Kent", "2026-06-01", 50)));
            // Assert: one of them planted the block; a refused transplant leaves its batch untouched
            assertEquals(List.of("A planting of Kent is already recorded on block " + blockCode),
                    messages(results, ConflictException.class), "trial " + trial);
            assertEquals(List.of(50), treeBalances(blockCode), "trial " + trial);
            boolean transplanted = results.get(0) instanceof NurseryEventResponse;
            NurseryBatchResponse batch = nurseryBatchService.getBatchById(batchId);
            assertEquals(transplanted ? 50 : 100, batch.currentCount(), "trial " + trial);
            assertEquals(transplanted ? 50 : 0, batch.transplantedCount(), "trial " + trial);
        }
    }

    @Test
    void twoTransplantsToTheSameBlock_shouldMoveOnlyOneBatch() throws Exception {
        for (int trial = 1; trial <= TRIALS; trial++) {
            // Arrange: two batches of the same variety
            String blockCode = "CQ" + trial;
            Long firstBatchId = startKentBatch("CQ-" + trial + "-1");
            Long secondBatchId = startKentBatch("CQ-" + trial + "-2");
            // Act
            List<Object> results = together(List.of(
                    transplant(firstBatchId, blockCode),
                    transplant(secondBatchId, blockCode)));
            // Assert: one transplant recorded, the other refused with nothing stored
            assertEquals(1, count(results, NurseryEventResponse.class), "trial " + trial);
            assertEquals(List.of("A planting of Kent is already recorded on block " + blockCode),
                    messages(results, ConflictException.class), "trial " + trial);
            assertEquals(List.of(50), treeBalances(blockCode), "trial " + trial);
            assertEquals(List.of(50, 100), List.of(firstBatchId, secondBatchId).stream()
                    .map(id -> nurseryBatchService.getBatchById(id).currentCount())
                    .sorted()
                    .toList(), "trial " + trial);
        }
    }
}
