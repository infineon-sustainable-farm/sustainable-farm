package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthFindingResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthTreatmentResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.NurseryBatchResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantAlertResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantAlertResponse.PlantAlert;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldForecastResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldForecastResponse.Entry;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldForecastResponse.VarietyWithoutReference;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerType;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerUnit;
import com.infineonbit.sustainablefarm.modules.plants.entity.GrowthPhaseYieldShare;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthFindingStatus;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueKind;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryOrigin;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryStage;
import com.infineonbit.sustainablefarm.modules.plants.entity.PlantAlertSeverity;
import com.infineonbit.sustainablefarm.modules.plants.entity.PlantAlertType;
import com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEvent;
import com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEventType;
import com.infineonbit.sustainablefarm.modules.plants.entity.TreatmentUnit;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import com.infineonbit.sustainablefarm.modules.plants.entity.VarietyReference;
import com.infineonbit.sustainablefarm.modules.plants.repository.GrowthPhaseYieldShareRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.PopulationEventRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.PopulationEventRepository.TreeBalance;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyReferenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The alerts are computed from the real forecast, on mocked repositories, and
 * from the answers of the other services, mocked: only their own rules are
 * tested here. Every alert is compared in full.
 */
@ExtendWith(MockitoExtension.class)
public class PlantAlertServiceTest {

    private static final LocalDate APRIL_1 = LocalDate.of(2027, 4, 1);
    private static final LocalDate MAY_15 = LocalDate.of(2027, 5, 15);
    private static final LocalDate KEITT_PLANTED_ON = LocalDate.of(2023, 9, 15);
    private static final String KEITT_SEASON_SOURCE = "varietal_guide_west_africa";

    @Mock
    private PopulationEventRepository populationEventRepository;

    @Mock
    private VarietyReferenceRepository varietyReferenceRepository;

    @Mock
    private GrowthPhaseYieldShareRepository growthPhaseYieldShareRepository;

    @Mock
    private HealthTreatmentService healthTreatmentService;

    @Mock
    private HealthFindingService healthFindingService;

    @Mock
    private FertilizerService fertilizerService;

    @Mock
    private NurseryBatchService nurseryBatchService;

    /** The real forecast and shares on the mocked repositories: the alerts follow their rules. */
    private YieldForecastService yieldForecastService() {
        return new YieldForecastService(populationEventRepository, varietyReferenceRepository,
                new GrowthPhaseYieldShareService(growthPhaseYieldShareRepository));
    }

    /** The service on a clock fixed at noon UTC of a day. */
    private PlantAlertService serviceOn(LocalDate today) {
        return serviceAt(Clock.fixed(today.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    private PlantAlertService serviceAt(Clock clock) {
        return new PlantAlertService(yieldForecastService(), healthTreatmentService, healthFindingService,
                fertilizerService, nurseryBatchService, clock);
    }

    private static Variety variety(long id, Integer farmId, String blockCode, String name) {
        Variety variety = new Variety();
        variety.setId(id);
        variety.setFarmId(farmId);
        variety.setBlockCode(blockCode);
        variety.setName(name);
        return variety;
    }

    private static PopulationEvent planting(Variety variety, LocalDate date) {
        return new PopulationEvent(
                variety.getId() + 100, variety, PopulationEventType.PLANTING, date, 40, "user_entry", null);
    }

    private static TreeBalance balance(long varietyId, long balance) {
        return new TreeBalance() {
            @Override
            public Long getVarietyId() {
                return varietyId;
            }

            @Override
            public Long getBalance() {
                return balance;
            }
        };
    }

    /** Stubs the plantings of the filters, each with the same number of trees left. */
    private void planted(Integer farmId, String blockCode, long treesLeft, PopulationEvent... plantings) {
        when(populationEventRepository.findPlantingsByOptionalFilters(farmId, blockCode))
                .thenReturn(List.of(plantings));
        when(populationEventRepository.findTreeBalances(anyCollection())).thenReturn(Arrays.stream(plantings)
                .map(planting -> balance(planting.getVariety().getId(), treesLeft))
                .toList());
    }

    private static VarietyReference reference(long id, String name, int harvestStartMonth, int harvestEndMonth,
                                              String seasonSource) {
        return new VarietyReference(id, name, 220.0, "Zalka_2025", harvestStartMonth, harvestEndMonth,
                seasonSource, null);
    }

    private static List<GrowthPhaseYieldShare> shares(double establishment, double gradual, double full) {
        return List.of(
                new GrowthPhaseYieldShare(1L, "establishment", establishment, "orchard_literature", null),
                new GrowthPhaseYieldShare(2L, "gradual production", gradual, "assumption_to_validate", null),
                new GrowthPhaseYieldShare(3L, "full production", full, "by_definition", null));
    }

    /** Stubs the agronomic reference. */
    private void reference(List<VarietyReference> varieties, List<GrowthPhaseYieldShare> shares) {
        when(varietyReferenceRepository.findAll()).thenReturn(varieties);
        when(growthPhaseYieldShareRepository.findAll()).thenReturn(shares);
    }

    /** Stubs the varieties of AgronomicReferenceLoader, Keitt from May to July, and share rows 0, 0.5 and 1. */
    private void defaultReference() {
        reference(List.of(
                        reference(1L, "Keitt", 5, 7, KEITT_SEASON_SOURCE),
                        reference(2L, "Kent", 4, 5, "FAO_mango_burkina"),
                        reference(3L, "Amelie", 2, 4, "FAO_mango_burkina")),
                shares(0.0, 0.5, 1.0));
    }

    /** Keitt planted on block C without farm in September 2023, with the default reference. */
    private Variety keittOnBlockC() {
        Variety keitt = variety(1L, null, "C", "Keitt");
        planted(null, null, 40, planting(keitt, KEITT_PLANTED_ON));
        defaultReference();
        return keitt;
    }

    private static HealthTreatmentResponse treatment(long id, Integer farmId, String blockCode, Long findingId,
                                                     LocalDate treatedOn, String productName, int intervalDays) {
        return new HealthTreatmentResponse(id, farmId, blockCode, "OTHER", "Other", "Fruit fly", findingId,
                treatedOn, productName, "deltamethrin", 1.5, TreatmentUnit.L, intervalDays,
                treatedOn.plusDays(intervalDays), "Awa", null, "user_entry", null);
    }

    private static FertilizerResponse fertilizer(long id, String name, FertilizerUnit unit, Double threshold,
                                                 double stock, boolean belowThreshold) {
        return new FertilizerResponse(id, name, FertilizerType.MINERAL, null, unit, threshold, stock,
                belowThreshold, "user_entry", null);
    }

    private static HealthFindingResponse finding(long id, LocalDate inspectedOn, String issueName,
                                                 String otherLabel, String treeLabel, HealthFindingStatus status) {
        boolean treated = status == HealthFindingStatus.IN_PROGRESS || status == HealthFindingStatus.TREATED;
        boolean resolved = status == HealthFindingStatus.TREATED
                || status == HealthFindingStatus.CLOSED_WITHOUT_TREATMENT;
        return new HealthFindingResponse(id, 50L + id, null, "C", inspectedOn,
                otherLabel == null ? "ANTHRACNOSE" : "OTHER", issueName,
                otherLabel == null ? HealthIssueKind.DISEASE : HealthIssueKind.OTHER,
                otherLabel, treeLabel, status, treated ? 1 : 0, null, null,
                resolved ? inspectedOn.plusDays(5) : null, null);
    }

    private static NurseryBatchResponse batch(long id, Integer farmId, String batchCode, String varietyName,
                                              NurseryStage stage, LocalDate stageSince, LocalDate plannedOn,
                                              String plannedBlockCode, int plantsLeft) {
        return new NurseryBatchResponse(id, farmId, batchCode, varietyName, NurseryOrigin.IN_HOUSE, null, null,
                LocalDate.of(2026, 10, 1), 120, plannedOn, plannedBlockCode, stage, stageSince,
                0, 120 - plantsLeft, plantsLeft, 120, 100.0, "user_entry", null);
    }

    private static PlantAlert harvestAlert(Integer farmId, String blockCode, String varietyName, String message,
                                           String date, String endDate, int days, long varietyId, String source) {
        return new PlantAlert(PlantAlertType.HARVEST_APPROACHING, PlantAlertSeverity.WARNING, farmId, blockCode,
                varietyName, message, LocalDate.parse(date), LocalDate.parse(endDate), days,
                varietyId, null, null, null, null, source);
    }

    private static PlantAlert intervalAlert(PlantAlertSeverity severity, Integer farmId, String message,
                                            String date, int days, long treatmentId, Long findingId) {
        return new PlantAlert(PlantAlertType.PRE_HARVEST_INTERVAL, severity, farmId, "C", null, message,
                LocalDate.parse(date), null, days, null, treatmentId, findingId, null, null, null);
    }

    private static PlantAlert stockAlert(PlantAlertSeverity severity, String message, long fertilizerId) {
        return new PlantAlert(PlantAlertType.LOW_STOCK, severity, null, null, null, message,
                null, null, null, null, null, null, fertilizerId, null, null);
    }

    private static PlantAlert issueAlert(String message, String date, int days, long findingId) {
        return new PlantAlert(PlantAlertType.OPEN_HEALTH_ISSUE, PlantAlertSeverity.WARNING, null, "C", null,
                message, LocalDate.parse(date), null, days, null, null, findingId, null, null, null);
    }

    private static PlantAlert batchAlert(Integer farmId, String blockCode, String varietyName, String message,
                                         String date, int days, long batchId) {
        return new PlantAlert(PlantAlertType.NURSERY_READY, PlantAlertSeverity.WARNING, farmId, blockCode,
                varietyName, message, LocalDate.parse(date), null, days, null, null, null, null, batchId, null);
    }

    private static List<PlantAlert> ofType(PlantAlertResponse response, PlantAlertType type) {
        return response.alerts().stream().filter(alert -> alert.type() == type).toList();
    }

    @Test
    void harvestApproaching_shouldWarn_whenTheSeasonStartsWithinTheWindow() {
        // Arrange
        keittOnBlockC();
        // Act
        PlantAlertResponse response = serviceOn(LocalDate.of(2027, 4, 7)).getAlerts(null, null, null);
        // Assert: the trees are 3 years old on 1 May 2027, gradual production
        assertEquals(List.of(harvestAlert(null, "C", "Keitt",
                        "Keitt on block C: harvest season starts on 1 May 2027, in 24 days",
                        "2027-05-01", "2027-07-31", 24, 1L, KEITT_SEASON_SOURCE)),
                response.alerts());
    }

    @Test
    void harvestApproaching_shouldCountTheLastDayOfTheWindow_butNotOneDayMore() {
        // Arrange: 30 days from 1 April to 1 May
        keittOnBlockC();
        // Act
        PlantAlertResponse within30 = serviceOn(APRIL_1).getAlerts(null, null, 30);
        PlantAlertResponse within29 = serviceOn(APRIL_1).getAlerts(null, null, 29);
        // Assert
        assertEquals(List.of(harvestAlert(null, "C", "Keitt",
                        "Keitt on block C: harvest season starts on 1 May 2027, in 30 days",
                        "2027-05-01", "2027-07-31", 30, 1L, KEITT_SEASON_SOURCE)),
                within30.alerts());
        assertTrue(within29.alerts().isEmpty());
    }

    @Test
    void harvestApproaching_shouldWarn_duringTheSeason_whateverTheWindow() {
        // Arrange
        keittOnBlockC();
        // Act
        PlantAlertResponse response = serviceOn(MAY_15).getAlerts(null, null, 1);
        // Assert
        assertEquals(List.of(harvestAlert(null, "C", "Keitt",
                        "Keitt on block C: harvest season in progress, from 1 May 2027 to 31 July 2027",
                        "2027-05-01", "2027-07-31", -14, 1L, KEITT_SEASON_SOURCE)),
                response.alerts());
    }

    @Test
    void harvestApproaching_shouldStaySilent_onceTheSeasonIsOver() {
        // Arrange: on 1 August, the next season starts in 274 days
        keittOnBlockC();
        // Act & Assert
        assertTrue(serviceOn(LocalDate.of(2027, 8, 1)).getAlerts(null, null, 90).alerts().isEmpty());
    }

    @Test
    void harvestApproaching_shouldStaySilent_whenTheTreesAreTooYoungForTheWholeSeason() {
        // Arrange: 2 years old from May to July 2026, in establishment, whose share is 0
        keittOnBlockC();
        // Act & Assert: before and during the 2026 season
        assertTrue(serviceOn(LocalDate.of(2026, 4, 1)).getAlerts(null, null, 90).alerts().isEmpty());
        assertTrue(serviceOn(LocalDate.of(2026, 5, 15)).getAlerts(null, null, 90).alerts().isEmpty());
    }

    @Test
    void harvestApproaching_shouldStartOnTheFirstProductiveMonth_whenTheTreesComeOfAgeDuringTheSeason() {
        // Arrange: Keitt planted in June 2024 is 2 years old in May and June 2027, and 3 years old in July
        LocalDate today = LocalDate.of(2027, 6, 15);
        Variety keitt = variety(1L, null, "C", "Keitt");
        planted(null, null, 40, planting(keitt, LocalDate.of(2024, 6, 15)));
        defaultReference();
        when(healthTreatmentService.getAllTreatments(null, null, null, today)).thenReturn(List.of(
                treatment(31L, null, "C", null, LocalDate.of(2027, 6, 10), "Insecticide B", 14)));
        // Act
        PlantAlertResponse response = serviceOn(today).getAlerts(null, null, null);
        YieldForecastResponse forecast = yieldForecastService().getYieldForecast(
                null, null, YearMonth.of(2027, 5), 3, today);
        // Assert: the season starts on 1 July for these trees, and the block is not in season yet
        assertEquals(List.of(
                        intervalAlert(PlantAlertSeverity.WARNING, null,
                                "Block C: harvest allowed from 24 June 2027, in 9 days "
                                        + "(Insecticide B applied on 10 June 2027, 14-day pre-harvest interval)",
                                "2027-06-24", 9, 31L, null),
                        harvestAlert(null, "C", "Keitt",
                                "Keitt on block C: harvest season starts on 1 July 2027, in 16 days",
                                "2027-07-01", "2027-07-31", 16, 1L, KEITT_SEASON_SOURCE)),
                response.alerts());
        // Assert: the forecast of the season gives a yield in July only
        assertEquals(List.of(YearMonth.of(2027, 7)), forecast.entries().stream().map(Entry::month).toList());
    }

    @Test
    void harvestApproaching_shouldOpenTheBlock_fromTheFirstProductiveMonth() {
        // Arrange: the same trees in July 2027, now in gradual production
        LocalDate today = LocalDate.of(2027, 7, 10);
        Variety keitt = variety(1L, null, "C", "Keitt");
        planted(null, null, 40, planting(keitt, LocalDate.of(2024, 6, 15)));
        defaultReference();
        when(healthTreatmentService.getAllTreatments(null, null, null, today)).thenReturn(List.of(
                treatment(32L, null, "C", null, LocalDate.of(2027, 7, 5), "Insecticide B", 14)));
        // Act
        PlantAlertResponse response = serviceOn(today).getAlerts(null, null, null);
        // Assert: the block is in season, so the interval is critical
        assertEquals(List.of(
                        intervalAlert(PlantAlertSeverity.CRITICAL, null,
                                "Block C, harvest season open: harvest allowed from 19 July 2027, in 9 days "
                                        + "(Insecticide B applied on 5 July 2027, 14-day pre-harvest interval)",
                                "2027-07-19", 9, 32L, null),
                        harvestAlert(null, "C", "Keitt",
                                "Keitt on block C: harvest season in progress, from 1 July 2027 to 31 July 2027",
                                "2027-07-01", "2027-07-31", -9, 1L, KEITT_SEASON_SOURCE)),
                response.alerts());
    }

    @Test
    void harvestApproaching_shouldStaySilent_forTreesPlantedDuringTheSeason() {
        // Arrange: planted on 10 May 2027, after the first day of the season, and in establishment until 2030
        Variety keitt = variety(1L, null, "C", "Keitt");
        planted(null, null, 40, planting(keitt, LocalDate.of(2027, 5, 10)));
        defaultReference();
        // Act & Assert
        assertTrue(serviceOn(MAY_15).getAlerts(null, null, 90).alerts().isEmpty());
    }

    @Test
    void harvestApproaching_shouldListAVarietyWithoutReference_insteadOfAnAlert() {
        // Arrange
        Variety palmer = variety(4L, null, "C", "Palmer");
        planted(null, null, 10, planting(palmer, LocalDate.of(2020, 1, 10)));
        defaultReference();
        // Act
        PlantAlertResponse response = serviceOn(MAY_15).getAlerts(null, null, null);
        // Assert
        assertTrue(response.alerts().isEmpty());
        assertEquals(List.of(new VarietyWithoutReference(null, "C", "Palmer", 10)),
                response.varietiesWithoutReference());
    }

    @Test
    void harvestApproaching_shouldIgnoreAVarietyWithNoTreeLeft() {
        // Arrange: every Keitt tree died
        Variety keitt = variety(1L, null, "C", "Keitt");
        planted(null, null, 0, planting(keitt, KEITT_PLANTED_ON));
        defaultReference();
        // Act
        PlantAlertResponse response = serviceOn(MAY_15).getAlerts(null, null, null);
        // Assert
        assertTrue(response.alerts().isEmpty());
        assertTrue(response.varietiesWithoutReference().isEmpty());
    }

    @Test
    void harvestApproaching_shouldFollowASeasonThatRunsOverTheNewYear() {
        // Arrange: a variety harvested from November to February, in full production
        Variety winter = variety(5L, 2, "C", "Winter");
        planted(null, null, 10, planting(winter, LocalDate.of(2015, 1, 1)));
        reference(List.of(reference(9L, "Winter", 11, 2, "test_source")), shares(0.0, 0.5, 1.0));
        // Act
        PlantAlertResponse response = serviceOn(LocalDate.of(2028, 1, 15)).getAlerts(null, null, null);
        // Assert
        assertEquals(List.of(harvestAlert(2, "C", "Winter",
                        "Winter on block C: harvest season in progress, from 1 November 2027 to 29 February 2028",
                        "2027-11-01", "2028-02-29", -75, 5L, "test_source")),
                response.alerts());
    }

    @Test
    void harvestApproaching_shouldUseTheDefaultShares_whenNoShareIsCorrected() {
        // Arrange: no share row at all, as outside the dev profile
        Variety keitt = variety(1L, null, "C", "Keitt");
        planted(null, null, 40, planting(keitt, KEITT_PLANTED_ON));
        reference(List.of(reference(1L, "Keitt", 5, 7, KEITT_SEASON_SOURCE)), List.of());
        // Act
        PlantAlertResponse establishing = serviceOn(APRIL_1.minusYears(1)).getAlerts(null, null, null);
        PlantAlertResponse gradual = serviceOn(APRIL_1).getAlerts(null, null, null);
        // Assert: nothing at 2 years (default 0), the season at 3 years (default 0.25)
        assertEquals(List.of(), establishing.alerts());
        assertEquals(List.of(harvestAlert(null, "C", "Keitt",
                        "Keitt on block C: harvest season starts on 1 May 2027, in 30 days",
                        "2027-05-01", "2027-07-31", 30, 1L, KEITT_SEASON_SOURCE)),
                gradual.alerts());
    }

    @Test
    void preHarvestInterval_shouldWarn_outsideTheHarvestSeasonOfTheBlock() {
        // Arrange: a treatment that answers finding 9, on 1 April
        keittOnBlockC();
        when(healthTreatmentService.getAllTreatments(null, null, null, APRIL_1)).thenReturn(List.of(
                treatment(31L, null, "C", 9L, LocalDate.of(2027, 3, 25), "Copper fungicide A", 14)));
        // Act
        PlantAlertResponse response = serviceOn(APRIL_1).getAlerts(null, null, null);
        // Assert
        assertEquals(List.of(intervalAlert(PlantAlertSeverity.WARNING, null,
                        "Block C: harvest allowed from 8 April 2027, in 7 days "
                                + "(Copper fungicide A applied on 25 March 2027, 14-day pre-harvest interval)",
                        "2027-04-08", 7, 31L, 9L)),
                ofType(response, PlantAlertType.PRE_HARVEST_INTERVAL));
    }

    @Test
    void preHarvestInterval_shouldBeCritical_duringTheHarvestSeasonOfTheBlock() {
        // Arrange: a preventive treatment on 8 May
        keittOnBlockC();
        when(healthTreatmentService.getAllTreatments(null, null, null, MAY_15)).thenReturn(List.of(
                treatment(32L, null, "C", null, LocalDate.of(2027, 5, 8), "Insecticide B", 14)));
        // Act
        PlantAlertResponse response = serviceOn(MAY_15).getAlerts(null, null, null);
        // Assert
        assertEquals(List.of(intervalAlert(PlantAlertSeverity.CRITICAL, null,
                        "Block C, harvest season open: harvest allowed from 22 May 2027, in 7 days "
                                + "(Insecticide B applied on 8 May 2027, 14-day pre-harvest interval)",
                        "2027-05-22", 7, 32L, null)),
                ofType(response, PlantAlertType.PRE_HARVEST_INTERVAL));
    }

    @Test
    void preHarvestInterval_shouldFollowTheSeasonOfTheSameFarm() {
        // Arrange: Keitt is in season on block C of farm 1; block C without farm is another block
        Variety keitt = variety(1L, 1, "C", "Keitt");
        planted(null, null, 40, planting(keitt, KEITT_PLANTED_ON));
        defaultReference();
        when(healthTreatmentService.getAllTreatments(null, null, null, MAY_15)).thenReturn(List.of(
                treatment(33L, null, "C", null, LocalDate.of(2027, 5, 8), "Insecticide B", 14),
                treatment(34L, 1, "C", null, LocalDate.of(2027, 5, 8), "Insecticide B", 14)));
        // Act
        List<PlantAlert> intervals = ofType(serviceOn(MAY_15).getAlerts(null, null, null),
                PlantAlertType.PRE_HARVEST_INTERVAL);
        // Assert: CRITICAL first
        assertEquals(List.of(34L, 33L), intervals.stream().map(PlantAlert::treatmentId).toList());
        assertEquals(List.of(PlantAlertSeverity.CRITICAL, PlantAlertSeverity.WARNING),
                intervals.stream().map(PlantAlert::severity).toList());
    }

    @Test
    void preHarvestInterval_shouldWarn_whenTheTreesOfTheBlockAreTooYoungToBeHarvested() {
        // Arrange: in May 2026, the Keitt trees of block C give no yield yet
        LocalDate today = LocalDate.of(2026, 5, 15);
        keittOnBlockC();
        when(healthTreatmentService.getAllTreatments(null, null, null, today)).thenReturn(List.of(
                treatment(35L, null, "C", null, LocalDate.of(2026, 5, 10), "Insecticide B", 14)));
        // Act
        PlantAlertResponse response = serviceOn(today).getAlerts(null, null, null);
        // Assert
        assertEquals(List.of(PlantAlertSeverity.WARNING), response.alerts().stream()
                .map(PlantAlert::severity).toList());
    }

    @Test
    void preHarvestInterval_shouldStaySilent_fromTheDayHarvestIsAllowed() {
        // Arrange: an interval that ends today, and an interval of 0 days
        when(healthTreatmentService.getAllTreatments(null, null, null, MAY_15)).thenReturn(List.of(
                treatment(36L, null, "C", null, LocalDate.of(2027, 4, 30), "Insecticide B", 15),
                treatment(37L, null, "C", null, MAY_15, "Sulfur", 0)));
        // Act & Assert
        assertTrue(serviceOn(MAY_15).getAlerts(null, null, null).alerts().isEmpty());
    }

    @Test
    void preHarvestInterval_shouldReadTheTreatmentsUpToToday() {
        // Act
        serviceOn(MAY_15).getAlerts(2, " C ", null);
        // Assert: the farm, the trimmed block, no first date, and today as the last one
        verify(healthTreatmentService).getAllTreatments(2, "C", null, MAY_15);
    }

    @Test
    void lowStock_shouldWarn_atOrBelowTheThreshold() {
        // Arrange: the API decides the badge; the quantities are written without trailing zeros
        when(fertilizerService.getAllFertilizers()).thenReturn(List.of(
                fertilizer(11L, "Foliar feed", FertilizerUnit.L, 12.5, 12.5, true),
                fertilizer(12L, "NPK 15-15-15", FertilizerUnit.KG, 50.0, 30.0, true)));
        // Act
        PlantAlertResponse response = serviceOn(MAY_15).getAlerts(null, null, null);
        // Assert
        assertEquals(List.of(
                        stockAlert(PlantAlertSeverity.WARNING,
                                "Foliar feed: 12.5 L left, at or below the alert threshold of 12.5 L", 11L),
                        stockAlert(PlantAlertSeverity.WARNING,
                                "NPK 15-15-15: 30 kg left, at or below the alert threshold of 50 kg", 12L)),
                response.alerts());
    }

    @Test
    void lowStock_shouldBeCritical_whenNothingIsLeft() {
        // Arrange
        when(fertilizerService.getAllFertilizers()).thenReturn(List.of(
                fertilizer(13L, "Urea", FertilizerUnit.KG, 50.0, 0.0, true)));
        // Act
        PlantAlertResponse response = serviceOn(MAY_15).getAlerts(null, null, null);
        // Assert
        assertEquals(List.of(stockAlert(PlantAlertSeverity.CRITICAL, "Urea: out of stock (alert threshold 50 kg)",
                13L)), response.alerts());
    }

    @Test
    void lowStock_shouldStaySilent_withoutTheLowStockBadge() {
        // Arrange: no threshold, or a stock above it
        when(fertilizerService.getAllFertilizers()).thenReturn(List.of(
                fertilizer(14L, "Compost", FertilizerUnit.KG, null, 0.0, false),
                fertilizer(15L, "DAP", FertilizerUnit.KG, 50.0, 80.0, false)));
        // Act & Assert
        assertTrue(serviceOn(MAY_15).getAlerts(null, null, null).alerts().isEmpty());
    }

    @Test
    void lowStock_shouldBeLeftOutForABlock_butKeptForAFarm() {
        // Arrange
        when(fertilizerService.getAllFertilizers()).thenReturn(List.of(
                fertilizer(13L, "Urea", FertilizerUnit.KG, 50.0, 0.0, true)));
        // Act
        PlantAlertResponse forBlock = serviceOn(MAY_15).getAlerts(null, "C", null);
        PlantAlertResponse forFarm = serviceOn(MAY_15).getAlerts(2, null, null);
        // Assert: a fertilizer belongs to no block and to no farm
        assertTrue(forBlock.alerts().isEmpty());
        assertEquals(List.of(13L), forFarm.alerts().stream().map(PlantAlert::fertilizerId).toList());
        verify(fertilizerService).getAllFertilizers();
    }

    @Test
    void openHealthIssue_shouldSayUntreated_whenTheProblemHasNoTreatment() {
        // Arrange
        when(healthFindingService.getAllFindings(null, null, null, null, null)).thenReturn(List.of(
                finding(7L, LocalDate.of(2027, 5, 3), "Anthracnose", null, null, HealthFindingStatus.UNTREATED)));
        // Act
        PlantAlertResponse response = serviceOn(MAY_15).getAlerts(null, null, null);
        // Assert
        assertEquals(List.of(issueAlert("Block C: Anthracnose seen on 3 May 2027, 12 days ago, untreated",
                "2027-05-03", -12, 7L)), response.alerts());
    }

    @Test
    void openHealthIssue_shouldSayTreated_whenTheProblemIsTreatedButNotResolved() {
        // Arrange
        when(healthFindingService.getAllFindings(null, null, null, null, null)).thenReturn(List.of(
                finding(8L, LocalDate.of(2027, 5, 3), "Anthracnose", null, null, HealthFindingStatus.IN_PROGRESS)));
        // Act
        PlantAlertResponse response = serviceOn(MAY_15).getAlerts(null, null, null);
        // Assert
        assertEquals(List.of(issueAlert(
                "Block C: Anthracnose seen on 3 May 2027, 12 days ago, treated, not resolved yet",
                "2027-05-03", -12, 8L)), response.alerts());
    }

    @Test
    void openHealthIssue_shouldStaySilent_onceTheProblemIsResolved() {
        // Arrange
        when(healthFindingService.getAllFindings(null, null, null, null, null)).thenReturn(List.of(
                finding(9L, LocalDate.of(2027, 5, 3), "Anthracnose", null, null, HealthFindingStatus.TREATED),
                finding(10L, LocalDate.of(2027, 5, 3), "Anthracnose", null, null,
                        HealthFindingStatus.CLOSED_WITHOUT_TREATMENT)));
        // Act & Assert
        assertTrue(serviceOn(MAY_15).getAlerts(null, null, null).alerts().isEmpty());
    }

    @Test
    void openHealthIssue_shouldNameTheLabelOfOther_andTheTree() {
        // Arrange: a problem outside the catalogue, on tree 42, seen today
        when(healthFindingService.getAllFindings(null, null, null, null, null)).thenReturn(List.of(
                finding(11L, MAY_15, "Other", "Sooty mould", "42", HealthFindingStatus.UNTREATED)));
        // Act
        PlantAlertResponse response = serviceOn(MAY_15).getAlerts(null, null, null);
        // Assert
        assertEquals(List.of(issueAlert("Block C, tree 42: Sooty mould seen on 15 May 2027, today, untreated",
                "2027-05-15", 0, 11L)), response.alerts());
    }

    @Test
    void nurseryReady_shouldWarn_whenTheBatchIsReadyBeforeItsPlannedDate() {
        // Arrange
        when(nurseryBatchService.getAllBatches(null, null)).thenReturn(List.of(
                batch(21L, 5, "P1", "Keitt", NurseryStage.READY_TO_TRANSPLANT, LocalDate.of(2027, 4, 20),
                        LocalDate.of(2027, 6, 15), "D", 120)));
        // Act
        PlantAlertResponse response = serviceOn(MAY_15).getAlerts(null, null, null);
        // Assert
        assertEquals(List.of(batchAlert(5, "D", "Keitt",
                "Batch P1 (Keitt, 120 plants): ready to transplant since 20 April 2027; "
                        + "transplant planned on 15 June 2027 to block D, in 31 days",
                "2027-06-15", 31, 21L)), response.alerts());
    }

    @Test
    void nurseryReady_shouldWarn_whenThePlannedDateHasCome_atAnyStage() {
        // Arrange: one plant left, no planned block
        when(nurseryBatchService.getAllBatches(null, null)).thenReturn(List.of(
                batch(22L, null, "P2", "Kent", NurseryStage.HARDENING, LocalDate.of(2027, 3, 1),
                        LocalDate.of(2027, 5, 1), null, 1)));
        // Act
        PlantAlertResponse response = serviceOn(MAY_15).getAlerts(null, null, null);
        // Assert
        assertEquals(List.of(batchAlert(null, null, "Kent",
                "Batch P2 (Kent, 1 plant): transplant planned on 1 May 2027, 14 days ago; current stage hardening",
                "2027-05-01", -14, 22L)), response.alerts());
    }

    @Test
    void nurseryReady_shouldStaySilent_whenNoPlantIsLeft() {
        // Arrange: transplanted in full, and lost in full
        when(nurseryBatchService.getAllBatches(null, null)).thenReturn(List.of(
                batch(23L, null, "P3", "Keitt", NurseryStage.READY_TO_TRANSPLANT, LocalDate.of(2027, 4, 20),
                        LocalDate.of(2027, 5, 1), "D", 0),
                batch(24L, null, "P4", "Kent", NurseryStage.GRAFTED, LocalDate.of(2026, 10, 1),
                        LocalDate.of(2027, 5, 1), null, 0)));
        // Act & Assert
        assertTrue(serviceOn(MAY_15).getAlerts(null, null, null).alerts().isEmpty());
    }

    @Test
    void nurseryReady_shouldStaySilent_whenNotReady_beforeThePlannedDate() {
        // Arrange
        when(nurseryBatchService.getAllBatches(null, null)).thenReturn(List.of(
                batch(25L, null, "P5", "Keitt", NurseryStage.GRAFTED, LocalDate.of(2026, 10, 1),
                        LocalDate.of(2027, 6, 15), "D", 120)));
        // Act & Assert
        assertTrue(serviceOn(MAY_15).getAlerts(null, null, null).alerts().isEmpty());
    }

    @Test
    void nurseryReady_shouldMatchABlockOnThePlannedBlock() {
        // Arrange: three ready batches, planned for D, for E, and for no block
        List<NurseryBatchResponse> batches = List.of(
                batch(26L, null, "P6", "Keitt", NurseryStage.READY_TO_TRANSPLANT, MAY_15,
                        LocalDate.of(2027, 6, 1), "D", 10),
                batch(27L, null, "P7", "Keitt", NurseryStage.READY_TO_TRANSPLANT, MAY_15,
                        LocalDate.of(2027, 6, 1), "E", 10),
                batch(28L, null, "P8", "Keitt", NurseryStage.READY_TO_TRANSPLANT, MAY_15,
                        LocalDate.of(2027, 6, 1), null, 10));
        when(nurseryBatchService.getAllBatches(null, null)).thenReturn(batches);
        // Act
        PlantAlertResponse forD = serviceOn(MAY_15).getAlerts(null, "D", null);
        PlantAlertResponse forAll = serviceOn(MAY_15).getAlerts(null, null, null);
        // Assert: the batch without planned block only shows without a block filter, first
        assertEquals(List.of(26L), forD.alerts().stream().map(PlantAlert::batchId).toList());
        assertEquals(List.of(28L, 26L, 27L), forAll.alerts().stream().map(PlantAlert::batchId).toList());
    }

    @Test
    void getAlerts_shouldAnswerEmptyLists_withTodayAndTheDefaultWindow() {
        // Act
        PlantAlertResponse response = serviceOn(MAY_15).getAlerts(null, null, null);
        // Assert
        assertEquals(new PlantAlertResponse(MAY_15, 30, List.of(), List.of()), response);
    }

    @Test
    void getAlerts_shouldPassTheFiltersToEverySource_andTurnABlankBlockIntoNoFilter() {
        // Act
        PlantAlertResponse filtered = serviceOn(MAY_15).getAlerts(2, " C ", 45);
        serviceOn(MAY_15).getAlerts(null, "  ", null);
        // Assert
        assertEquals(45, filtered.withinDays());
        verify(populationEventRepository).findPlantingsByOptionalFilters(2, "C");
        verify(healthTreatmentService).getAllTreatments(2, "C", null, MAY_15);
        verify(healthFindingService).getAllFindings(2, "C", null, null, null);
        verify(nurseryBatchService).getAllBatches(2, null);
        verify(populationEventRepository).findPlantingsByOptionalFilters(null, null);
        verify(fertilizerService).getAllFertilizers();
    }

    @Test
    void getAlerts_shouldSortTheAlerts_criticalFirst_thenByDate() {
        // Arrange: block C on 15 May 2027, as in the scenario of the integration test
        keittOnBlockC();
        when(healthTreatmentService.getAllTreatments(null, null, null, MAY_15)).thenReturn(List.of(
                treatment(32L, null, "C", null, LocalDate.of(2027, 5, 8), "Insecticide B", 14)));
        when(fertilizerService.getAllFertilizers()).thenReturn(List.of(
                fertilizer(13L, "Urea", FertilizerUnit.KG, 50.0, 0.0, true)));
        when(healthFindingService.getAllFindings(null, null, null, null, null)).thenReturn(List.of(
                finding(7L, LocalDate.of(2027, 5, 3), "Anthracnose", null, null, HealthFindingStatus.UNTREATED)));
        when(nurseryBatchService.getAllBatches(null, null)).thenReturn(List.of(
                batch(21L, 5, "P1", "Keitt", NurseryStage.READY_TO_TRANSPLANT, LocalDate.of(2027, 4, 20),
                        LocalDate.of(2027, 6, 15), "D", 120)));
        // Act
        PlantAlertResponse response = serviceOn(MAY_15).getAlerts(null, null, null);
        // Assert
        assertEquals(List.of(
                        "CRITICAL PRE_HARVEST_INTERVAL 2027-05-22",
                        "CRITICAL LOW_STOCK null",
                        "WARNING HARVEST_APPROACHING 2027-05-01",
                        "WARNING OPEN_HEALTH_ISSUE 2027-05-03",
                        "WARNING NURSERY_READY 2027-06-15"),
                response.alerts().stream()
                        .map(alert -> alert.severity() + " " + alert.type() + " " + alert.date())
                        .toList());
    }

    @Test
    void getAlerts_shouldTakeTheDayOfTheClock_inItsTimeZone() {
        // Arrange: 23:30 UTC on 30 April is already 1 May at UTC+1
        keittOnBlockC();
        Clock clock = Clock.fixed(Instant.parse("2027-04-30T23:30:00Z"), ZoneOffset.ofHours(1));
        // Act
        PlantAlertResponse response = serviceAt(clock).getAlerts(null, null, null);
        // Assert: the season starts on that day
        assertEquals(LocalDate.of(2027, 5, 1), response.today());
        assertEquals(List.of(0), response.alerts().stream().map(PlantAlert::daysFromToday).toList());
    }
}
