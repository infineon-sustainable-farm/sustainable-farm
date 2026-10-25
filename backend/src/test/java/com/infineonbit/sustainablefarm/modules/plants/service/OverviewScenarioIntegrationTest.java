package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Request.ApplicationRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.FertilizerRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.HarvestRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PlantingRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PreventiveTreatmentRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PurchaseRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.VarietyReferenceRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HarvestStatusResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HarvestStatusResponse.VarietyHarvestStatus;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantAlertResponse.PlantAlert;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantsOverviewResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantsOverviewResponse.MaturityShare;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldComparisonResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldComparisonResponse.BlockComparison;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldComparisonResponse.Comparison;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldComparisonResponse.NotCompared;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldComparisonResponse.PhaseShare;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldComparisonResponse.VarietyComparison;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueKind;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueReference;
import com.infineonbit.sustainablefarm.modules.plants.entity.MaturityStatus;
import com.infineonbit.sustainablefarm.modules.plants.entity.PlantAlertSeverity;
import com.infineonbit.sustainablefarm.modules.plants.entity.PlantAlertType;
import com.infineonbit.sustainablefarm.modules.plants.repository.HealthIssueReferenceRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyReferenceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The four reads of the remaining Plants requirements on the database, for
 * farm 7 on 15 June 2027: the last purchase date of a fertilizer, the yield
 * comparison, the harvest status and the overview. The expected values were
 * written before the code.
 *
 * <p>Keitt is planted on block A in September 2023 (40 trees, in gradual
 * production in 2027) and on block B in March 2026 (100 trees, in
 * establishment until 2029); Brooks, absent from the reference, on block C in
 * January 2020 (30 trees). Block A is picked twice and treated on 10 June 2027
 * with a 14-day pre-harvest interval; block C is picked once.
 *
 * <p>The API refuses records dated in the future, so the records are written
 * through the services, and the reads come from services whose clock is fixed
 * on the checked day, as in the alerts scenario. Farm 7, the fertilizers "NPK
 * 12-12-17" and "Cattle manure" and the catalogue row STEM_END_ROT are used by
 * no other test; the catalogue row is added if missing and kept. A fertilizer
 * belongs to no farm, so the stock alerts of the other tests' fertilizers
 * count in the overview of farm 7 too: they are added to the expected count.
 * The Keitt reference of the dev loader is entered when missing and removed
 * afterwards; the growth phases keep their default shares.
 */
@SpringBootTest
@ActiveProfiles("test")
class OverviewScenarioIntegrationTest {

    private static final int FARM = 7;
    private static final String STEM_END_ROT = "STEM_END_ROT";
    private static final String KEITT_SEASON_SOURCE = "varietal_guide_west_africa";
    private static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");
    private static final LocalDate JUNE_15 = LocalDate.of(2027, 6, 15);
    private static final LocalDate JUNE_24 = LocalDate.of(2027, 6, 24);

    @Autowired
    private YieldForecastService yieldForecastService;

    @Autowired
    private HarvestService harvestService;

    @Autowired
    private HealthTreatmentService healthTreatmentService;

    @Autowired
    private HealthFindingService healthFindingService;

    @Autowired
    private FertilizerService fertilizerService;

    @Autowired
    private FertilizerMovementService fertilizerMovementService;

    @Autowired
    private NurseryBatchService nurseryBatchService;

    @Autowired
    private PlantingService plantingService;

    @Autowired
    private HealthIssueReferenceRepository healthIssueReferenceRepository;

    @Autowired
    private VarietyReferenceService varietyReferenceService;

    @Autowired
    private VarietyReferenceRepository varietyReferenceRepository;

    /** The Keitt reference entered by this test, or {@code null} when one was already there. */
    private Long keittReferenceOfTheTest;

    @BeforeEach
    void addStemEndRotIfMissing() {
        if (healthIssueReferenceRepository.findByCode(STEM_END_ROT).isEmpty()) {
            healthIssueReferenceRepository.save(new HealthIssueReference(null, STEM_END_ROT, "Stem-end rot",
                    HealthIssueKind.DISEASE, null, null, "user_entry", NOW));
        }
    }

    @BeforeEach
    void addTheKeittReferenceIfMissing() {
        boolean missing = varietyReferenceService.getAllReferences().stream()
                .noneMatch(reference -> VarietyReferenceMatcher.matchKey(reference.varietyName()).equals("keitt"));
        keittReferenceOfTheTest = missing
                ? varietyReferenceService.createReference(new VarietyReferenceRequest(
                        "Keitt", 220.0, "Zalka_2025", 5, 7, KEITT_SEASON_SOURCE)).id()
                : null;
    }

    @AfterEach
    void removeTheKeittReferenceOfTheTest() {
        if (keittReferenceOfTheTest != null) {
            varietyReferenceRepository.deleteById(keittReferenceOfTheTest);
        }
    }

    private static Clock noonOf(LocalDate day) {
        return Clock.fixed(day.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    }

    private PlantAlertService alertsOn(LocalDate day) {
        return new PlantAlertService(yieldForecastService, healthTreatmentService, healthFindingService,
                fertilizerService, nurseryBatchService, noonOf(day));
    }

    private HarvestStatusService statusOn(LocalDate day) {
        return new HarvestStatusService(yieldForecastService, healthTreatmentService, harvestService, noonOf(day));
    }

    private PlantsOverviewService overviewOn(LocalDate day) {
        return new PlantsOverviewService(yieldForecastService, statusOn(day), alertsOn(day), noonOf(day));
    }

    private Long plant(String blockCode, String varietyName, LocalDate plantedOn, int trees) {
        return plantingService.recordPlanting(new PlantingRequest(FARM, blockCode, varietyName, plantedOn, trees))
                .varietyId();
    }

    private void harvest(String blockCode, String varietyName, LocalDate harvestedOn, double quantityKg) {
        harvestService.recordHarvest(new HarvestRequest(FARM, blockCode, varietyName, harvestedOn, quantityKg));
    }

    @Test
    void reads_shouldGiveTheValuesOfTheScenario_on15June2027() {
        // Arrange: the plantings, the harvests and the treatment
        Long keittA = plant("A", "Keitt", LocalDate.of(2023, 9, 15), 40);
        Long keittB = plant("B", "Keitt", LocalDate.of(2026, 3, 1), 100);
        Long brooksC = plant("C", "Brooks", LocalDate.of(2020, 1, 10), 30);
        harvest("A", "Keitt", LocalDate.of(2027, 5, 20), 300.0);
        harvest("A", "Keitt", LocalDate.of(2027, 6, 10), 250.5);
        harvest("C", "Brooks", LocalDate.of(2027, 6, 5), 120.0);
        Long treatment = healthTreatmentService.recordPreventiveTreatment(new PreventiveTreatmentRequest(FARM, "A",
                STEM_END_ROT, null, LocalDate.of(2027, 6, 10), "Copper fungicide A", "copper oxychloride", 2.0,
                "KG", 14.0, "Awa", null)).id();
        // Arrange: the NPK bought twice, applied, then bought again with a late entry dated in March
        Long npk = fertilizerService.createFertilizer(
                new FertilizerRequest("NPK 12-12-17", "MINERAL", "12-12-17", "KG", null)).id();
        Long manure = fertilizerService.createFertilizer(
                new FertilizerRequest("Cattle manure", "ORGANIC", null, "KG", null)).id();
        fertilizerMovementService.recordPurchase(npk,
                new PurchaseRequest(LocalDate.of(2027, 2, 1), 200.0, "Supplier A", null, null));
        fertilizerMovementService.recordPurchase(npk,
                new PurchaseRequest(LocalDate.of(2027, 4, 12), 100.0, "Supplier A", null, null));
        fertilizerMovementService.recordApplication(npk,
                new ApplicationRequest(LocalDate.of(2027, 5, 2), 160.0, FARM, "A", "Awa", null));
        fertilizerMovementService.recordPurchase(npk,
                new PurchaseRequest(LocalDate.of(2027, 3, 1), 50.0, "Supplier B", null, null));

        // 1. Last purchase: neither the later application nor the late entry moves it
        assertEquals(LocalDate.of(2027, 4, 12), fertilizerService.getFertilizerById(npk).lastPurchaseDate());
        assertEquals(190.0, fertilizerService.getFertilizerById(npk).currentStock());
        assertNull(fertilizerService.getFertilizerById(manure).lastPurchaseDate());

        // 2. Yield comparison over 2027: 3 × 733.3 kg expected on A, nothing on B, Brooks apart
        YieldComparisonResponse comparison = new YieldComparisonService(yieldForecastService, harvestService,
                noonOf(JUNE_15)).getYieldComparison(FARM, null, null, null);
        assertEquals(YearMonth.of(2027, 1), comparison.from());
        assertEquals(12, comparison.months());
        assertEquals(List.of(
                new VarietyComparison(FARM, "A", keittA, "Keitt", 40, 2199.9, 550.5, 2, 25.0, 220.0, "Zalka_2025",
                        KEITT_SEASON_SOURCE, List.of(new PhaseShare("gradual production", 0.25, "Bally_2002"))),
                new VarietyComparison(FARM, "B", keittB, "Keitt", 100, 0.0, null, 0, null, 220.0, "Zalka_2025",
                        KEITT_SEASON_SOURCE, List.of(new PhaseShare("establishment", 0.0, "orchard_literature")))),
                comparison.varieties());
        assertEquals(List.of(new BlockComparison(FARM, "A", 2199.9, 550.5, 25.0),
                new BlockComparison(FARM, "B", 0.0, null, null)), comparison.blocks());
        assertEquals(new Comparison(2199.9, 550.5, 25.0), comparison.total());
        assertEquals(List.of(new NotCompared(FARM, "C", brooksC, "Brooks", 30, 120.0, 1, "NO_REFERENCE")),
                comparison.notCompared());

        // 3. Harvest status: A ripe while the interval runs, B in 686 days, no status for Brooks
        HarvestStatusResponse status = statusOn(JUNE_15).getHarvestStatus(FARM, null, null);
        assertEquals(List.of(
                new VarietyHarvestStatus(FARM, "A", keittA, "Keitt", 40, MaturityStatus.RIPE,
                        LocalDate.of(2027, 5, 1), LocalDate.of(2027, 7, 31), -45, JUNE_24, treatment,
                        LocalDate.of(2027, 5, 20), KEITT_SEASON_SOURCE, "Bally_2002"),
                new VarietyHarvestStatus(FARM, "B", keittB, "Keitt", 100, MaturityStatus.IMMATURE,
                        LocalDate.of(2029, 5, 1), LocalDate.of(2029, 7, 31), 686, null, null, null,
                        KEITT_SEASON_SOURCE, "Bally_2002"),
                new VarietyHarvestStatus(FARM, "C", brooksC, "Brooks", 30, null, null, null, null, null, null,
                        null, null, null)), status.varieties());

        // 4. Overview: 170 trees, 100 in growth stage, none ready, the 2 alerts of farm 7, and the trees of
        // the harvest status by maturity status
        List<PlantAlert> alerts = alertsOn(JUNE_15).getAlerts(FARM, null, null).alerts();
        assertEquals(List.of("CRITICAL PRE_HARVEST_INTERVAL A", "WARNING HARVEST_APPROACHING A"),
                alerts.stream()
                        .filter(alert -> Integer.valueOf(FARM).equals(alert.farmId()))
                        .map(alert -> alert.severity() + " " + alert.type() + " " + alert.blockCode())
                        .toList());
        List<PlantAlert> stockAlertsOfOtherTests = alerts.stream()
                .filter(alert -> alert.type() == PlantAlertType.LOW_STOCK)
                .toList();
        int criticalStockAlerts = (int) stockAlertsOfOtherTests.stream()
                .filter(alert -> alert.severity() == PlantAlertSeverity.CRITICAL)
                .count();
        assertEquals(new PlantsOverviewResponse(JUNE_15, 170, 100, 0, 2 + stockAlertsOfOtherTests.size(),
                1 + criticalStockAlerts, List.of(
                        new MaturityShare(MaturityStatus.IMMATURE, 100, 58.8),
                        new MaturityShare(MaturityStatus.RIPE, 40, 23.5),
                        new MaturityShare(MaturityStatus.READY_FOR_HARVEST, 0, 0.0),
                        new MaturityShare(null, 30, 17.6))),
                overviewOn(JUNE_15).getOverview(FARM, null));

        // On 24 June 2027 the interval is over: A is ready, as it would be without the treatment
        VarietyHarvestStatus keittAOnJune24 = statusOn(JUNE_24).getHarvestStatus(FARM, "A", null).varieties().get(0);
        assertEquals(MaturityStatus.READY_FOR_HARVEST, keittAOnJune24.maturityStatus());
        assertNull(keittAOnJune24.harvestAllowedFrom());
        PlantsOverviewResponse overviewOnJune24 = overviewOn(JUNE_24).getOverview(FARM, null);
        assertEquals(40, overviewOnJune24.readyForHarvest());
        assertEquals(List.of(
                new MaturityShare(MaturityStatus.IMMATURE, 100, 58.8),
                new MaturityShare(MaturityStatus.RIPE, 0, 0.0),
                new MaturityShare(MaturityStatus.READY_FOR_HARVEST, 40, 23.5),
                new MaturityShare(null, 30, 17.6)), overviewOnJune24.maturity());
    }
}
