package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.CurrencyRateRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.FertilizerRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PurchaseRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerMovementResponse;
import com.infineonbit.sustainablefarm.modules.plants.repository.CurrencyRateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The EUR to XOF rate as the user enters it, through the real services, their
 * transactions and the database. Outside the dev profile no rate exists at
 * startup; each test starts and ends without one.
 */
@SpringBootTest
@ActiveProfiles("test")
class CurrencyRateIntegrationTest {

    /** Each race runs this many times, since H2 does not reproduce it on every trial. */
    private static final int TRIALS = 10;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private CurrencyRateService currencyRateService;

    @Autowired
    private CurrencyRateRepository currencyRateRepository;

    @Autowired
    private FertilizerService fertilizerService;

    @Autowired
    private FertilizerMovementService fertilizerMovementService;

    @BeforeEach
    @AfterEach
    void removeTheRate() {
        currencyRateRepository.deleteAll();
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

    private static boolean isFirstRate(Object result) {
        return result instanceof CurrencyRateService.SavedRate savedRate && savedRate.created();
    }

    @Test
    void startup_shouldLoadNoRate_outsideTheDevProfile() {
        // Assert: the loader is not even a bean, so no default rate reaches another environment
        assertTrue(applicationContext.getBeansOfType(CurrencyRateLoader.class).isEmpty());
    }

    @Test
    void aPurchaseRecordedWithoutRate_shouldGetItsFcfaAmount_onceTheRateIsEntered() {
        // Arrange
        Long id = fertilizerService.createFertilizer(
                new FertilizerRequest("Rate test NPK", "MINERAL", null, "KG", null)).id();
        // Act: a purchase in euros while no rate exists, then the first rate
        FertilizerMovementResponse recorded = fertilizerMovementService.recordPurchase(id,
                new PurchaseRequest(LocalDate.of(2026, 10, 1), 50.0, "Supplier A", 120.0, "EUR"));
        CurrencyRateService.SavedRate savedRate =
                currencyRateService.saveEurToXof(new CurrencyRateRequest(655.957, null));
        FertilizerMovementResponse read =
                fertilizerMovementService.getAllMovements(id, null, null, null, null, null).get(0);
        // Assert: stored without its FCFA amount, which the next read gives: 120 x 655.957 = 78714.84
        assertNull(recorded.totalCostXof());
        assertEquals(120.0, recorded.totalCostEur());
        assertTrue(savedRate.created());
        assertEquals("user_entry", savedRate.rate().source());
        assertEquals(78715L, read.totalCostXof());
        assertEquals(120.0, read.totalCostEur());
    }

    @Test
    void twoFirstRatesSentTogether_shouldKeepOneRow_andRefuseOrReplaceTheOther() throws Exception {
        for (int trial = 1; trial <= TRIALS; trial++) {
            // Arrange: no rate yet
            currencyRateRepository.deleteAll();
            // Act
            List<Object> results = together(List.of(
                    () -> currencyRateService.saveEurToXof(new CurrencyRateRequest(655.957, null)),
                    () -> currencyRateService.saveEurToXof(new CurrencyRateRequest(656.0, null))));
            // Assert: one is the first rate; the other got the 409, or replaced it once the first was committed
            assertEquals(1, results.stream().filter(CurrencyRateIntegrationTest::isFirstRate).count(),
                    "trial " + trial + ": " + results);
            Object other = results.stream().filter(result -> !isFirstRate(result)).findFirst().orElseThrow();
            assertTrue(other instanceof ConflictException || other instanceof CurrencyRateService.SavedRate,
                    "trial " + trial + ": " + other);
            assertEquals(1, currencyRateRepository.count(), "trial " + trial);
        }
    }
}
