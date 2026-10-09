package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.GrowthPhaseYieldShareRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PlantingRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.VarietyReferenceRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.GrowthPhaseYieldShareResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantAlertResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.VarietyReferenceResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldForecastResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldForecastResponse.VarietyWithoutReference;
import com.infineonbit.sustainablefarm.modules.plants.entity.PlantAlertType;
import com.infineonbit.sustainablefarm.modules.plants.repository.GrowthPhaseYieldShareRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyReferenceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The agronomic reference as the user enters it, through the real services,
 * their transactions and the database. Outside the dev profile nothing is
 * loaded at startup: each test starts and ends with no correction of the
 * shares and without the varieties it enters.
 *
 * <p>Farm 6, block R and the varieties "Sensation" and "Race mango" are used
 * by no other test.
 */
@SpringBootTest
@ActiveProfiles("test")
class AgronomicReferenceIntegrationTest {

    /** Each race runs this many times, since H2 does not reproduce it on every trial. */
    private static final int TRIALS = 10;

    private static final int FARM = 6;
    private static final String SENSATION = "Sensation";
    private static final String RACE_MANGO = "Race mango";
    private static final LocalDate APRIL_1_2026 = LocalDate.of(2026, 4, 1);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private VarietyReferenceService varietyReferenceService;

    @Autowired
    private VarietyReferenceRepository varietyReferenceRepository;

    @Autowired
    private GrowthPhaseYieldShareService growthPhaseYieldShareService;

    @Autowired
    private GrowthPhaseYieldShareRepository growthPhaseYieldShareRepository;

    @Autowired
    private YieldForecastService yieldForecastService;

    @Autowired
    private PlantingService plantingService;

    @Autowired
    private HealthTreatmentService healthTreatmentService;

    @Autowired
    private HealthFindingService healthFindingService;

    @Autowired
    private FertilizerService fertilizerService;

    @Autowired
    private NurseryBatchService nurseryBatchService;

    @BeforeEach
    @AfterEach
    void removeTheValuesOfTheTests() {
        growthPhaseYieldShareRepository.deleteAll();
        removeReferences(SENSATION, RACE_MANGO);
    }

    private void removeReferences(String... names) {
        Set<String> removed = Set.of(names);
        varietyReferenceService.getAllReferences().stream()
                .filter(reference -> removed.contains(reference.varietyName()))
                .forEach(reference -> varietyReferenceRepository.deleteById(reference.id()));
    }

    private long referencesNamed(String name) {
        return varietyReferenceService.getAllReferences().stream()
                .filter(reference -> reference.varietyName().equals(name))
                .count();
    }

    /** The forecast of farm 6 for 2027, one line per entry. */
    private List<String> forecastEntries2027() {
        return yieldForecastService.getYieldForecast(FARM, null, YearMonth.of(2027, 1), 12).entries().stream()
                .map(entry -> entry.month() + " " + entry.expectedKg() + " kg, share " + entry.phaseShare()
                        + " " + entry.phaseShareSource() + ", yield " + entry.yieldSource())
                .toList();
    }

    /** The harvest alerts of farm 6 on a day, from a service whose clock is fixed on that day. */
    private List<String> harvestAlertsOn(LocalDate day) {
        Clock clock = Clock.fixed(day.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        PlantAlertResponse response = new PlantAlertService(yieldForecastService, healthTreatmentService,
                healthFindingService, fertilizerService, nurseryBatchService, clock).getAlerts(FARM, null, null);
        return response.alerts().stream()
                .filter(alert -> alert.type() == PlantAlertType.HARVEST_APPROACHING)
                .map(alert -> alert.date() + " " + alert.endDate() + " " + alert.daysFromToday() + " | "
                        + alert.message())
                .toList();
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

    @Test
    void startup_shouldLoadNoReference_outsideTheDevProfile() {
        // Assert: the loader is not even a bean, so no default reference reaches another environment
        assertTrue(applicationContext.getBeansOfType(AgronomicReferenceLoader.class).isEmpty());
    }

    @Test
    void theForecastAndTheAlerts_shouldFollowEachEntryAndCorrection_atOnce() {
        // S0: Sensation planted on block R in September 2023, before its reference is entered
        plantingService.recordPlanting(new PlantingRequest(FARM, "R", SENSATION, LocalDate.of(2023, 9, 15), 40));
        YieldForecastResponse withoutReference =
                yieldForecastService.getYieldForecast(FARM, null, YearMonth.of(2027, 1), 12);
        assertEquals(List.of(), withoutReference.entries());
        assertEquals(List.of(new VarietyWithoutReference(FARM, "R", SENSATION, 40)),
                withoutReference.varietiesWithoutReference());

        // S1: its reference entered; the trees are 3 years old in 2027, gradual production by default
        Long id = varietyReferenceService.createReference(
                new VarietyReferenceRequest(SENSATION, 220.0, null, 5, 7, null)).id();
        assertEquals(List.of(
                        "2027-05 733.3 kg, share 0.25 Bally_2002, yield user_entry",
                        "2027-06 733.3 kg, share 0.25 Bally_2002, yield user_entry",
                        "2027-07 733.3 kg, share 0.25 Bally_2002, yield user_entry"),
                forecastEntries2027());

        // S2: the share of gradual production corrected, then the yield of the variety
        growthPhaseYieldShareService.saveShare("GRADUAL_PRODUCTION", new GrowthPhaseYieldShareRequest(0.3, null));
        assertEquals(List.of(
                        "2027-05 880.0 kg, share 0.3 user_entry, yield user_entry",
                        "2027-06 880.0 kg, share 0.3 user_entry, yield user_entry",
                        "2027-07 880.0 kg, share 0.3 user_entry, yield user_entry"),
                forecastEntries2027());
        varietyReferenceService.updateReference(id,
                new VarietyReferenceRequest(SENSATION, 240.0, "farm_records_2026", 5, 7, null));
        assertEquals(List.of(
                        "2027-05 960.0 kg, share 0.3 user_entry, yield farm_records_2026",
                        "2027-06 960.0 kg, share 0.3 user_entry, yield farm_records_2026",
                        "2027-07 960.0 kg, share 0.3 user_entry, yield farm_records_2026"),
                forecastEntries2027());

        // S3, 1 April 2026: the trees are 2 years old, so no harvest until establishment gives a share
        assertEquals(List.of(), harvestAlertsOn(APRIL_1_2026));
        growthPhaseYieldShareService.saveShare("ESTABLISHMENT", new GrowthPhaseYieldShareRequest(0.1, null));
        assertEquals(List.of("2026-05-01 2026-07-31 30 | "
                        + "Sensation on block R: harvest season starts on 1 May 2026, in 30 days"),
                harvestAlertsOn(APRIL_1_2026));
    }

    @Test
    void twoFirstCorrectionsSentTogether_shouldKeepOneRow_andRefuseOrReplaceTheOther() throws Exception {
        for (int trial = 1; trial <= TRIALS; trial++) {
            // Arrange: no correction yet
            growthPhaseYieldShareRepository.deleteAll();
            // Act
            List<Object> results = together(List.of(
                    () -> growthPhaseYieldShareService.saveShare(
                            "GRADUAL_PRODUCTION", new GrowthPhaseYieldShareRequest(0.3, null)),
                    () -> growthPhaseYieldShareService.saveShare(
                            "GRADUAL_PRODUCTION", new GrowthPhaseYieldShareRequest(0.35, null))));
            // Assert: one row; the other request got the 409, or replaced the share once the first was committed
            assertTrue(results.stream().anyMatch(GrowthPhaseYieldShareResponse.class::isInstance),
                    "trial " + trial + ": " + results);
            assertTrue(results.stream().allMatch(result -> result instanceof GrowthPhaseYieldShareResponse
                    || result instanceof ConflictException), "trial " + trial + ": " + results);
            assertEquals(1, growthPhaseYieldShareRepository.count(), "trial " + trial);
        }
    }

    @Test
    void twoIdenticalNamesSentTogether_shouldKeepOneRow_andRefuseTheOther() throws Exception {
        for (int trial = 1; trial <= TRIALS; trial++) {
            // Arrange: no "Race mango" yet
            removeReferences(RACE_MANGO);
            VarietyReferenceRequest request = new VarietyReferenceRequest(RACE_MANGO, 150.0, null, 3, 5, null);
            // Act
            List<Object> results = together(List.of(
                    () -> varietyReferenceService.createReference(request),
                    () -> varietyReferenceService.createReference(request)));
            // Assert: one row, and the other request refused with the name
            assertEquals(1, results.stream().filter(VarietyReferenceResponse.class::isInstance).count(),
                    "trial " + trial + ": " + results);
            ConflictException refused = (ConflictException) results.stream()
                    .filter(ConflictException.class::isInstance).findFirst().orElseThrow();
            assertEquals("A variety reference named Race mango already exists", refused.getMessage());
            assertEquals(1, referencesNamed(RACE_MANGO), "trial " + trial);
        }
    }
}
