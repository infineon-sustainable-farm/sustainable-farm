package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.CurrencyRateRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.GrowthPhaseYieldShareRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PlantScenarioRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.ScenarioVariantParams;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantScenarioResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.ScenarioAnalysisResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.ScenarioAnalysisResponse.Amount;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.ScenarioAnalysisResponse.BenefitLine;
import com.infineonbit.sustainablefarm.modules.plants.repository.CurrencyRateRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.GrowthPhaseYieldShareRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.PlantScenarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A scenario as the user enters it, through the real services, their
 * transactions and the database. Outside the dev profile nothing is loaded at
 * startup: each test starts and ends without the scenarios it enters, without
 * a correction of the shares and without an exchange rate.
 *
 * <p>The scenarios "Integration plan" and "Race plan" are used by no other test.
 */
@SpringBootTest
@ActiveProfiles("test")
class ScenarioAnalysisIntegrationTest {

    /** Each race runs this many times, since H2 does not reproduce it on every trial. */
    private static final int TRIALS = 10;

    private static final String INTEGRATION_PLAN = "Integration plan";
    private static final String RACE_PLAN = "Race plan";
    private static final ScenarioVariantParams TWELVE_YEARS = new ScenarioVariantParams(null, null, null, null, 12);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private PlantScenarioService plantScenarioService;

    @Autowired
    private PlantScenarioRepository plantScenarioRepository;

    @Autowired
    private ScenarioAnalysisService scenarioAnalysisService;

    @Autowired
    private GrowthPhaseYieldShareService growthPhaseYieldShareService;

    @Autowired
    private GrowthPhaseYieldShareRepository growthPhaseYieldShareRepository;

    @Autowired
    private CurrencyRateService currencyRateService;

    @Autowired
    private CurrencyRateRepository currencyRateRepository;

    @BeforeEach
    @AfterEach
    void removeTheValuesOfTheTests() {
        growthPhaseYieldShareRepository.deleteAll();
        currencyRateRepository.deleteAll();
        removeScenarios(INTEGRATION_PLAN, RACE_PLAN);
    }

    private void removeScenarios(String... names) {
        Set<String> removed = Set.of(names);
        plantScenarioService.getAllScenarios().stream()
                .filter(scenario -> removed.contains(scenario.name()))
                .forEach(scenario -> plantScenarioRepository.deleteById(scenario.id()));
    }

    private long scenariosNamed(String name) {
        return plantScenarioService.getAllScenarios().stream()
                .filter(scenario -> scenario.name().equals(name))
                .count();
    }

    /** The values of the Zalka study, under another name, without loss rate nor cost per added tree. */
    private static PlantScenarioRequest plan(String name, Double lossRate, Double costPerAdditionalTree) {
        return new PlantScenarioRequest(name, "Keitt", 200, null, 220.0, null, lossRate, null, 0.9072, null,
                "EUR", 6707.0, null, null, null, 171512.0, null, costPerAdditionalTree, null);
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
    void startup_shouldLoadNoScenario_outsideTheDevProfile() {
        // Assert: the loader is not even a bean, so the Zalka figures reach no other environment
        assertTrue(applicationContext.getBeansOfType(PlantScenarioLoader.class).isEmpty());
    }

    @Test
    void theAnalysis_shouldFollowEachEntryAndCorrection_atOnce() {
        // S0: the plan entered, no exchange rate yet: the amounts are in euros only
        Long id = plantScenarioService.createScenario(plan(INTEGRATION_PLAN, null, null)).id();
        ScenarioAnalysisResponse entered = scenarioAnalysisService.getAnalysis(id, TWELVE_YEARS);
        assertNull(entered.eurToXofRate());
        assertEquals(new Amount(null, 39916.8), entered.baseline().revenue());
        assertEquals(12, entered.baseline().paybackYear());
        assertEquals(List.of("lossRate", "variableCostPerKg", "costPerAdditionalTree"), entered.missingInputs());

        // S1: the share of gradual production corrected to a half: 22,000 kg in year 4, payback a year earlier
        growthPhaseYieldShareService.saveShare("GRADUAL_PRODUCTION", new GrowthPhaseYieldShareRequest(0.5, null));
        ScenarioAnalysisResponse corrected = scenarioAnalysisService.getAnalysis(id, TWELVE_YEARS);
        assertEquals(22000.0, corrected.baseline().years().get(3).harvestKg());
        assertEquals(11, corrected.baseline().paybackYear());

        // S2: the exchange rate entered: every amount also in FCFA
        currencyRateService.saveEurToXof(new CurrencyRateRequest(655.957, null));
        ScenarioAnalysisResponse converted = scenarioAnalysisService.getAnalysis(id, TWELVE_YEARS);
        assertEquals(655.957, converted.eurToXofRate());
        assertEquals(new Amount(26183704L, 39916.8), converted.baseline().revenue());

        // S3: a loss rate and a cost per added tree entered, then 20 trees more and fewer losses simulated
        PlantScenarioResponse completed = plantScenarioService.updateScenario(id, plan(INTEGRATION_PLAN, 0.1, 12.5));
        assertEquals("user_entry", completed.lossRateSource());
        ScenarioAnalysisResponse variant = scenarioAnalysisService.getAnalysis(id,
                new ScenarioVariantParams(20, null, 0.05, 10000.0, null));
        assertEquals(39600.0, variant.baseline().sellableKg());
        assertEquals(17.0, variant.baseline().roiPct());
        assertEquals(45980.0, variant.variant().sellableKg());
        assertEquals(List.of(
                        new BenefitLine("TREE_COUNT", 3960.0, 0.9072, new Amount(2356532L, 3592.51)),
                        new BenefitLine("LOSS_RATE", 2420.0, 0.9072, new Amount(1440108L, 2195.43))),
                variant.change().benefitLines());
        assertEquals(new Amount(3796640L, 5787.94), variant.change().annualBenefit());
        assertEquals(new Amount(6723559L, 10250.0), variant.change().additionalInvestment());
        assertEquals(1.8, variant.change().additionalInvestmentPaybackYears());
        assertEquals(19.3, variant.variant().roiPct());
    }

    @Test
    void twoIdenticalNamesSentTogether_shouldKeepOneRow_andRefuseTheOther() throws Exception {
        for (int trial = 1; trial <= TRIALS; trial++) {
            // Arrange: no "Race plan" yet
            removeScenarios(RACE_PLAN);
            PlantScenarioRequest request = plan(RACE_PLAN, null, null);
            // Act
            List<Object> results = together(List.of(
                    () -> plantScenarioService.createScenario(request),
                    () -> plantScenarioService.createScenario(request)));
            // Assert: one row, and the other request refused with the name
            assertEquals(1, results.stream().filter(PlantScenarioResponse.class::isInstance).count(),
                    "trial " + trial + ": " + results);
            ConflictException refused = (ConflictException) results.stream()
                    .filter(ConflictException.class::isInstance).findFirst().orElseThrow();
            assertEquals("A scenario named Race plan already exists", refused.getMessage());
            assertEquals(1, scenariosNamed(RACE_PLAN), "trial " + trial);
        }
    }
}
