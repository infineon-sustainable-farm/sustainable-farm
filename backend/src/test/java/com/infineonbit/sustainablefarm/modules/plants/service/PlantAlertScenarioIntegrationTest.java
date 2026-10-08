package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Request.ApplicationRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.FertilizerRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.HealthFindingRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.HealthInspectionRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.NurseryBatchRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PlantingRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PreventiveTreatmentRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PurchaseRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.StageChangeRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.TransplantRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantAlertResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantAlertResponse.PlantAlert;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueKind;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueReference;
import com.infineonbit.sustainablefarm.modules.plants.entity.PlantAlertSeverity;
import com.infineonbit.sustainablefarm.modules.plants.entity.PlantAlertType;
import com.infineonbit.sustainablefarm.modules.plants.repository.HealthIssueReferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The alerts of farm 5 over a year, on the database: Keitt planted on block
 * C in September 2023, a fertilizer below its threshold then out of stock,
 * two treatments, a problem left untreated and a nursery batch made ready.
 * The expected values were written before the code.
 *
 * <p>The API refuses records dated in the future, so the records are written
 * through the services, step by step, and the alerts are read from a service
 * whose clock is fixed on each checked day. Each check sees only what was
 * recorded before it: the stock depends on the recorded movements, not on
 * their dates.
 *
 * <p>Farm 5, the fertilizer "NPK 15-15-15" and the catalogue row SOOTY_MOULD
 * are used by no other test. The catalogue row is added if missing and kept,
 * so that no test that removes "Other" meets a finding of this one. A
 * fertilizer belongs to no farm, so only the stock alerts of this test's
 * fertilizer are read.
 */
@SpringBootTest
@ActiveProfiles("test")
class PlantAlertScenarioIntegrationTest {

    private static final int FARM = 5;
    private static final String SOOTY_MOULD = "SOOTY_MOULD";
    private static final Instant NOW = Instant.parse("2026-10-08T10:00:00Z");
    private static final LocalDate APRIL_1 = LocalDate.of(2027, 4, 1);
    private static final LocalDate MAY_15 = LocalDate.of(2027, 5, 15);

    private static final String INTERVAL_IN_APRIL = "WARNING PRE_HARVEST_INTERVAL C null 2027-04-08 null 7 | "
            + "Block C: harvest allowed from 8 April 2027, in 7 days "
            + "(Copper fungicide A applied on 25 March 2027, 14-day pre-harvest interval)";
    private static final String SEASON_IN_30_DAYS = "WARNING HARVEST_APPROACHING C Keitt 2027-05-01 2027-07-31 30 | "
            + "Keitt on block C: harvest season starts on 1 May 2027, in 30 days";
    private static final String STOCK_30_KG = "WARNING LOW_STOCK null null null null null | "
            + "NPK 15-15-15: 30 kg left, at or below the alert threshold of 50 kg";
    private static final String INTERVAL_IN_MAY = "CRITICAL PRE_HARVEST_INTERVAL C null 2027-05-22 null 7 | "
            + "Block C, harvest season open: harvest allowed from 22 May 2027, in 7 days "
            + "(Insecticide B applied on 8 May 2027, 14-day pre-harvest interval)";
    private static final String OUT_OF_STOCK = "CRITICAL LOW_STOCK null null null null null | "
            + "NPK 15-15-15: out of stock (alert threshold 50 kg)";
    private static final String SEASON_IN_PROGRESS = "WARNING HARVEST_APPROACHING C Keitt 2027-05-01 2027-07-31 -14 | "
            + "Keitt on block C: harvest season in progress, from 1 May 2027 to 31 July 2027";
    private static final String MOULD_12_DAYS_AGO = "WARNING OPEN_HEALTH_ISSUE C null 2027-05-03 null -12 | "
            + "Block C: Sooty mould seen on 3 May 2027, 12 days ago, untreated";
    private static final String BATCH_READY = "WARNING NURSERY_READY D Keitt 2027-06-15 null 31 | "
            + "Batch P1 (Keitt, 120 plants): ready to transplant since 20 April 2027; "
            + "transplant planned on 15 June 2027 to block D, in 31 days";
    private static final String MOULD_90_DAYS_AGO = "WARNING OPEN_HEALTH_ISSUE C null 2027-05-03 null -90 | "
            + "Block C: Sooty mould seen on 3 May 2027, 90 days ago, untreated";

    @Autowired
    private PlantAlertService plantAlertService;

    @Autowired
    private YieldForecastService yieldForecastService;

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
    private NurseryEventService nurseryEventService;

    @Autowired
    private PlantingService plantingService;

    @Autowired
    private HealthInspectionService healthInspectionService;

    @Autowired
    private HealthIssueReferenceRepository healthIssueReferenceRepository;

    @BeforeEach
    void addSootyMouldIfMissing() {
        if (healthIssueReferenceRepository.findByCode(SOOTY_MOULD).isEmpty()) {
            healthIssueReferenceRepository.save(new HealthIssueReference(null, SOOTY_MOULD, "Sooty mould",
                    HealthIssueKind.DISEASE, null, null, "user_entry", NOW));
        }
    }

    /** The alerts of farm 5 on a day, from a service whose clock is fixed on that day. */
    private PlantAlertResponse alertsOn(LocalDate day, String blockCode, Integer withinDays) {
        Clock clock = Clock.fixed(day.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        return new PlantAlertService(yieldForecastService, healthTreatmentService, healthFindingService,
                fertilizerService, nurseryBatchService, clock).getAlerts(FARM, blockCode, withinDays);
    }

    /** The alerts of this test: those of farm 5, and the stock alerts of its fertilizer. */
    private static List<PlantAlert> ours(PlantAlertResponse response, Long fertilizerId) {
        return response.alerts().stream()
                .filter(alert -> Integer.valueOf(FARM).equals(alert.farmId())
                        || (fertilizerId != null && fertilizerId.equals(alert.fertilizerId())))
                .toList();
    }

    /** The alerts of this test, one line each. */
    private static List<String> lines(PlantAlertResponse response, Long fertilizerId) {
        return ours(response, fertilizerId).stream()
                .map(alert -> alert.severity() + " " + alert.type() + " " + alert.blockCode() + " "
                        + alert.varietyName() + " " + alert.date() + " " + alert.endDate() + " "
                        + alert.daysFromToday() + " | " + alert.message())
                .toList();
    }

    @Test
    void alerts_shouldFollowTheKeittScenarioOfBlockC() {
        // S0: Keitt planted on block C in September 2023
        Long keitt = plantingService.recordPlanting(
                new PlantingRequest(FARM, "C", "Keitt", LocalDate.of(2023, 9, 15), 40)).varietyId();

        // A0, 1 April 2026: the season starts in 30 days, but the trees are 2 years old then
        PlantAlertResponse checkA0 = alertsOn(LocalDate.of(2026, 4, 1), null, null);
        assertEquals(List.of(), lines(checkA0, null));
        assertEquals(List.of(), checkA0.varietiesWithoutReference());

        // S1: the NPK below its threshold, a treatment in March, batch P1 at the grafted stage
        Long npk = fertilizerService.createFertilizer(
                new FertilizerRequest("NPK 15-15-15", "MINERAL", "15-15-15", "KG", 50.0)).id();
        fertilizerMovementService.recordPurchase(npk,
                new PurchaseRequest(LocalDate.of(2027, 3, 1), 100.0, "Supplier A", null, null));
        fertilizerMovementService.recordApplication(npk,
                new ApplicationRequest(LocalDate.of(2027, 3, 20), 70.0, FARM, "C", "Awa", null));
        healthTreatmentService.recordPreventiveTreatment(new PreventiveTreatmentRequest(FARM, "C", SOOTY_MOULD,
                null, LocalDate.of(2027, 3, 25), "Copper fungicide A", "copper oxychloride", 2.0, "KG", 14.0,
                "Awa", null));
        Long batch = nurseryBatchService.createBatch(new NurseryBatchRequest(FARM, "P1", "Keitt", "IN_HOUSE",
                null, null, LocalDate.of(2026, 10, 1), 120.0, "GRAFTED", LocalDate.of(2027, 6, 15), "D")).id();

        // A, 1 April 2027: the harvest season starts in exactly 30 days, the last day of the window
        PlantAlertResponse checkA = alertsOn(APRIL_1, null, null);
        assertEquals(APRIL_1, checkA.today());
        assertEquals(30, checkA.withinDays());
        assertEquals(List.of(INTERVAL_IN_APRIL, SEASON_IN_30_DAYS, STOCK_30_KG), lines(checkA, npk));
        assertEquals(List.of(INTERVAL_IN_APRIL, STOCK_30_KG), lines(alertsOn(APRIL_1, null, 29), npk));

        // S2: P1 ready, sooty mould left untreated, the rest of the NPK applied, a treatment in the season
        nurseryEventService.recordStageChange(batch,
                new StageChangeRequest("READY_TO_TRANSPLANT", LocalDate.of(2027, 4, 20)));
        Long finding = healthInspectionService.recordInspection(new HealthInspectionRequest(FARM, "C",
                        LocalDate.of(2027, 5, 3), 70.0, "Awa", "VISUAL",
                        List.of(new HealthFindingRequest(SOOTY_MOULD, null, null))))
                .findings().get(0).id();
        fertilizerMovementService.recordApplication(npk,
                new ApplicationRequest(LocalDate.of(2027, 5, 5), 30.0, FARM, "C", "Awa", null));
        Long treatment = healthTreatmentService.recordPreventiveTreatment(new PreventiveTreatmentRequest(FARM, "C",
                SOOTY_MOULD, null, LocalDate.of(2027, 5, 8), "Insecticide B", "deltamethrin", 1.5, "L", 14.0,
                "Awa", null)).id();

        // B, 15 May 2027: every type, in full
        PlantAlertResponse checkB = alertsOn(MAY_15, null, null);
        assertEquals(List.of(INTERVAL_IN_MAY, OUT_OF_STOCK, SEASON_IN_PROGRESS, MOULD_12_DAYS_AGO, BATCH_READY),
                lines(checkB, npk));
        List<PlantAlert> alerts = ours(checkB, npk);
        assertEquals(new PlantAlert(PlantAlertType.PRE_HARVEST_INTERVAL, PlantAlertSeverity.CRITICAL, FARM, "C",
                null, alerts.get(0).message(), LocalDate.of(2027, 5, 22), null, 7,
                null, treatment, null, null, null, null), alerts.get(0));
        assertEquals(new PlantAlert(PlantAlertType.LOW_STOCK, PlantAlertSeverity.CRITICAL, null, null,
                null, alerts.get(1).message(), null, null, null,
                null, null, null, npk, null, null), alerts.get(1));
        assertEquals(new PlantAlert(PlantAlertType.HARVEST_APPROACHING, PlantAlertSeverity.WARNING, FARM, "C",
                "Keitt", alerts.get(2).message(), LocalDate.of(2027, 5, 1), LocalDate.of(2027, 7, 31), -14,
                keitt, null, null, null, null, "varietal_guide_west_africa"), alerts.get(2));
        assertEquals(new PlantAlert(PlantAlertType.OPEN_HEALTH_ISSUE, PlantAlertSeverity.WARNING, FARM, "C",
                null, alerts.get(3).message(), LocalDate.of(2027, 5, 3), null, -12,
                null, null, finding, null, null, null), alerts.get(3));
        assertEquals(new PlantAlert(PlantAlertType.NURSERY_READY, PlantAlertSeverity.WARNING, FARM, "D",
                "Keitt", alerts.get(4).message(), LocalDate.of(2027, 6, 15), null, 31,
                null, null, null, null, batch, null), alerts.get(4));
        assertEquals(List.of(), checkB.varietiesWithoutReference());
        // B, filtered on a block: no stock alert; the batch goes with its planned block
        assertEquals(List.of(INTERVAL_IN_MAY, SEASON_IN_PROGRESS, MOULD_12_DAYS_AGO),
                lines(alertsOn(MAY_15, "C", null), npk));
        assertEquals(List.of(BATCH_READY), lines(alertsOn(MAY_15, "D", null), npk));

        // S3: the 120 plants of P1 transplanted on block D
        nurseryEventService.recordTransplant(batch, new TransplantRequest(LocalDate.of(2027, 6, 20), 120.0, "D"));

        // C, 1 August 2027: the season is over, the next one is 274 days away; P1 has no plant left
        PlantAlertResponse checkC = alertsOn(LocalDate.of(2027, 8, 1), null, null);
        assertEquals(List.of(OUT_OF_STOCK, MOULD_90_DAYS_AGO), lines(checkC, npk));
        assertEquals(List.of(), checkC.varietiesWithoutReference());
    }

    @Test
    void plantAlertService_shouldTakeTodayFromTheSystemClock() {
        // Act: the service built by Spring, with the Plants clock
        LocalDate before = LocalDate.now();
        LocalDate today = plantAlertService.getAlerts(FARM, "NO-SUCH-BLOCK", null).today();
        LocalDate after = LocalDate.now();
        // Assert
        assertTrue(!today.isBefore(before) && !today.isAfter(after), today + " outside " + before + ".." + after);
    }
}
