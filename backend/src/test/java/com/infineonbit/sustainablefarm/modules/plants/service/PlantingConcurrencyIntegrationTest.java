package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PlantingRequest;
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
 * reproduce a race on every trial, so a single trial could pass by chance.
 */
@SpringBootTest
@ActiveProfiles("test")
class PlantingConcurrencyIntegrationTest {

    private static final int TRIALS = 10;

    @Autowired
    private PlantingService plantingService;

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
        return () -> plantingService.recordPlanting(request);
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
            assertEquals(List.of(LocalDate.of(2021, 3, 1)),
                    growthCalendarRepository.findByFarmAndBlock(null, blockCode).stream()
                            .map(GrowthCalendar::getPlantingDate)
                            .toList(),
                    "trial " + trial);
        }
    }
}
