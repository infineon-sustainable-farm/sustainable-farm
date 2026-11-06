package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Request.MaturityObservationRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PlantingRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.VarietyReferenceRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HarvestStatusResponse.FieldObservation;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HarvestStatusResponse.VarietyHarvestStatus;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.MaturityObservationResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantsOverviewResponse.MaturityShare;
import com.infineonbit.sustainablefarm.modules.plants.entity.MaturityMethod;
import com.infineonbit.sustainablefarm.modules.plants.entity.MaturityStatus;
import com.infineonbit.sustainablefarm.modules.plants.repository.MaturityObservationRepository;
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
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Maturity observations through the real services and the database. The
 * variety of an observation is a lazy association and open-in-view is
 * disabled, so the list is built after the session of the read is closed; a
 * mocked repository cannot show that the variety is loaded with the
 * observations. In CI the database is PostgreSQL, so the measured value is
 * read back from its numeric column there.
 *
 * <p>The API refuses observations dated in the future, so the observations of
 * 2027 are written through the service, and the harvest status and the
 * overview are read from services whose clock is fixed on the checked day, as
 * in the overview scenario. Farm 8 and its blocks are used by no other test,
 * and each test reads its own blocks. The Keitt reference of the dev loader
 * is entered when missing and removed afterwards.
 */
@SpringBootTest
@ActiveProfiles("test")
class MaturityObservationIntegrationTest {

    private static final int FARM = 8;
    private static final String KEITT_SEASON_SOURCE = "varietal_guide_west_africa";

    @Autowired
    private PlantingService plantingService;

    @Autowired
    private MaturityObservationService maturityObservationService;

    @Autowired
    private MaturityObservationRepository maturityObservationRepository;

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
    private NurseryBatchService nurseryBatchService;

    @Autowired
    private VarietyReferenceService varietyReferenceService;

    @Autowired
    private VarietyReferenceRepository varietyReferenceRepository;

    /** The Keitt reference entered by this test, or {@code null} when one was already there. */
    private Long keittReferenceOfTheTest;

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

    private HarvestStatusService statusOn(LocalDate day) {
        return new HarvestStatusService(yieldForecastService, healthTreatmentService, harvestService,
                maturityObservationService, noonOf(day));
    }

    private PlantsOverviewService overviewOn(LocalDate day) {
        PlantAlertService alerts = new PlantAlertService(yieldForecastService, healthTreatmentService,
                healthFindingService, fertilizerService, nurseryBatchService, noonOf(day));
        return new PlantsOverviewService(yieldForecastService, statusOn(day), alerts, noonOf(day));
    }

    private Long plant(String blockCode, LocalDate plantedOn, int trees) {
        return plantingService.recordPlanting(new PlantingRequest(FARM, blockCode, "Keitt", plantedOn, trees))
                .varietyId();
    }

    private MaturityObservationResponse observe(String blockCode, LocalDate observedOn, String status) {
        return maturityObservationService.recordObservation(new MaturityObservationRequest(FARM, blockCode, "Keitt",
                observedOn, status, "VISUAL", null, "Awa"));
    }

    @Test
    void getAllObservations_shouldReturnEveryField_withTheVarietyLoadedWithTheObservations() {
        // Arrange: Keitt planted on block MO1 of farm 8, then a Brix reading sent in any case
        Long varietyId = plant("MO1", LocalDate.of(2015, 3, 1), 40);
        Long observationId = maturityObservationService.recordObservation(new MaturityObservationRequest(FARM,
                " mo1 ", " keitt ", LocalDate.of(2026, 10, 12), "ready_for_harvest", "brix", 14.2, " Awa ")).id();
        // The write time as stored, at the precision of the database
        Instant lastUpdated = maturityObservationRepository.findById(observationId).orElseThrow().getLastUpdated();
        // Act
        List<MaturityObservationResponse> observations =
                maturityObservationService.getAllObservations(FARM, "MO1", null, null);
        // Assert: every field, the value with its unit, the variety as planted
        assertEquals(List.of(new MaturityObservationResponse(observationId, FARM, "MO1", varietyId, "Keitt",
                LocalDate.of(2026, 10, 12), MaturityStatus.READY_FOR_HARVEST, MaturityMethod.BRIX, 14.2, "°Bx",
                "Awa", "user_entry", lastUpdated)), observations);
    }

    @Test
    void getHarvestStatus_shouldLetTheObservationCount_untilTheLastDayOfItsSeason() {
        // Arrange: Keitt on MO2 planted in September 2023, in gradual production in 2027; its window is open
        // from 1 May, but the flesh is still white on 10 May
        Long varietyId = plant("MO2", LocalDate.of(2023, 9, 15), 40);
        MaturityObservationResponse immature = observe("MO2", LocalDate.of(2027, 5, 10), "IMMATURE");
        // Act
        VarietyHarvestStatus onMay12 = statusOn(LocalDate.of(2027, 5, 12)).getHarvestStatus(FARM, "MO2", null)
                .varieties().get(0);
        VarietyHarvestStatus onJuly31 = statusOn(LocalDate.of(2027, 7, 31)).getHarvestStatus(FARM, "MO2", null)
                .varieties().get(0);
        VarietyHarvestStatus onAugust1 = statusOn(LocalDate.of(2027, 8, 1)).getHarvestStatus(FARM, "MO2", null)
                .varieties().get(0);
        // Assert: on 12 May the observation holds, with the estimate and its dates beside it
        assertEquals(new VarietyHarvestStatus(FARM, "MO2", varietyId, "Keitt", 40, MaturityStatus.IMMATURE,
                "field_observation", MaturityStatus.READY_FOR_HARVEST,
                new FieldObservation(immature.id(), LocalDate.of(2027, 5, 10), MaturityStatus.IMMATURE,
                        MaturityMethod.VISUAL, null, null, "Awa", "user_entry", LocalDate.of(2027, 7, 31)),
                LocalDate.of(2027, 5, 1), LocalDate.of(2027, 7, 31), -11, null, null, null, KEITT_SEASON_SOURCE,
                "Bally_2002"), onMay12);
        // Assert: it holds on the last day of the season, and not the day after
        assertEquals(MaturityStatus.IMMATURE, onJuly31.maturityStatus());
        assertEquals("field_observation", onJuly31.statusSource());
        assertNull(onAugust1.observation());
        assertEquals("estimate", onAugust1.statusSource());
        assertEquals(MaturityStatus.IMMATURE, onAugust1.estimatedStatus());
        assertEquals(LocalDate.of(2028, 5, 1), onAugust1.estimatedHarvestDate());
    }

    @Test
    void getOverview_shouldCountTheTreesObservedReadyForHarvest() {
        // Arrange: Keitt on MO3, ready by the estimate on 15 June 2027; young Keitt on MO4, immature by the
        // estimate, but its first fruits are seen ready on 12 June
        plant("MO3", LocalDate.of(2023, 9, 15), 40);
        plant("MO4", LocalDate.of(2026, 3, 1), 100);
        LocalDate june15 = LocalDate.of(2027, 6, 15);
        assertEquals(0, overviewOn(june15).getOverview(FARM, "MO4").readyForHarvest());
        // Act
        observe("MO4", LocalDate.of(2027, 6, 12), "READY_FOR_HARVEST");
        // Assert: the observed trees count, in the figure and in the trees by maturity status, and the estimate
        // of MO3 still counts
        assertEquals(100, overviewOn(june15).getOverview(FARM, "MO4").readyForHarvest());
        assertEquals(List.of(
                new MaturityShare(MaturityStatus.IMMATURE, 0, 0.0),
                new MaturityShare(MaturityStatus.RIPE, 0, 0.0),
                new MaturityShare(MaturityStatus.READY_FOR_HARVEST, 100, 100.0),
                new MaturityShare(null, 0, 0.0)), overviewOn(june15).getOverview(FARM, "MO4").maturity());
        assertEquals(40, overviewOn(june15).getOverview(FARM, "MO3").readyForHarvest());
        assertEquals(MaturityStatus.IMMATURE,
                statusOn(june15).getHarvestStatus(FARM, "MO4", null).varieties().get(0).estimatedStatus());
    }
}
