package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.ApplicationRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.FertilizerRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.LossRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PurchaseRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerMovementResponse;
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
 * Guards the fertilizer stock against movements sent at the same time: a
 * stock of 50 kg and two movements of 30 kg must end with one of them
 * recorded, the other refused with the usual 422, and 20 kg left.
 *
 * <p>Each case runs {@value #TRIALS} times on new fertilizers, since H2 does
 * not reproduce a race on every trial.
 */
@SpringBootTest
@ActiveProfiles("test")
class FertilizerStockConcurrencyIntegrationTest {

    private static final int TRIALS = 10;
    private static final LocalDate JUNE_1 = LocalDate.of(2026, 6, 1);
    private static final LocalDate JUNE_15 = LocalDate.of(2026, 6, 15);

    @Autowired
    private FertilizerService fertilizerService;

    @Autowired
    private FertilizerMovementService fertilizerMovementService;

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

    private static List<String> refusals(List<Object> results) {
        return results.stream().filter(BusinessRuleException.class::isInstance)
                .map(result -> ((Exception) result).getMessage())
                .toList();
    }

    /** A new fertilizer, in kilograms, with 50 kg bought. */
    private Long fertilizerWith50Kg(String name) {
        Long id = fertilizerService.createFertilizer(new FertilizerRequest(name, "MINERAL", null, "KG", null)).id();
        fertilizerMovementService.recordPurchase(id, new PurchaseRequest(JUNE_1, 50.0, "Supplier A", null, null));
        return id;
    }

    private Callable<Object> apply30Kg(Long id) {
        return () -> fertilizerMovementService.recordApplication(id,
                new ApplicationRequest(JUNE_15, 30.0, null, "B", "Team A", null));
    }

    private Callable<Object> lose30Kg(Long id) {
        return () -> fertilizerMovementService.recordLoss(id, new LossRequest(JUNE_15, 30.0, "expired"));
    }

    @Test
    void twoApplicationsSentTogether_shouldRecordOneAndRefuseTheOther() throws Exception {
        for (int trial = 1; trial <= TRIALS; trial++) {
            // Arrange
            String name = "Concurrency NPK " + trial;
            Long id = fertilizerWith50Kg(name);
            // Act
            List<Object> results = together(List.of(apply30Kg(id), apply30Kg(id)));
            // Assert: the second one sees the first, and the stock never goes below zero
            assertEquals(1, count(results, FertilizerMovementResponse.class), "trial " + trial);
            assertEquals(List.of("Not enough stock of " + name + ": 20 kg left, 30 kg requested"),
                    refusals(results), "trial " + trial);
            assertEquals(20.0, fertilizerService.getFertilizerById(id).currentStock(), "trial " + trial);
        }
    }

    @Test
    void applicationAndLossSentTogether_shouldRecordOneAndRefuseTheOther() throws Exception {
        for (int trial = 1; trial <= TRIALS; trial++) {
            // Arrange
            String name = "Concurrency Urea " + trial;
            Long id = fertilizerWith50Kg(name);
            // Act
            List<Object> results = together(List.of(apply30Kg(id), lose30Kg(id)));
            // Assert
            assertEquals(1, count(results, FertilizerMovementResponse.class), "trial " + trial);
            assertEquals(List.of("Not enough stock of " + name + ": 20 kg left, 30 kg requested"),
                    refusals(results), "trial " + trial);
            assertEquals(20.0, fertilizerService.getFertilizerById(id).currentStock(), "trial " + trial);
        }
    }
}
