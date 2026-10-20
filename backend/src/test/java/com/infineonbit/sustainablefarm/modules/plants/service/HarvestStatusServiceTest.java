package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HarvestResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HarvestStatusResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HarvestStatusResponse.VarietyHarvestStatus;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthTreatmentResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantAlertResponse.PlantAlert;
import com.infineonbit.sustainablefarm.modules.plants.entity.GrowthPhaseYieldShare;
import com.infineonbit.sustainablefarm.modules.plants.entity.MaturityStatus;
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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The statuses are computed from the real forecast, on mocked repositories,
 * and from the treatments and the harvests of their services, mocked. The
 * growth phases keep their default shares, unless a test corrects them. Every
 * line is compared in full.
 */
@ExtendWith(MockitoExtension.class)
public class HarvestStatusServiceTest {

    private static final int FARM = 7;
    private static final LocalDate JUNE_15_2027 = LocalDate.of(2027, 6, 15);
    private static final LocalDate KEITT_A_PLANTED_ON = LocalDate.of(2023, 9, 15);
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
    private HarvestService harvestService;

    @Mock
    private HealthFindingService healthFindingService;

    @Mock
    private FertilizerService fertilizerService;

    @Mock
    private NurseryBatchService nurseryBatchService;

    private final List<PopulationEvent> plantings = new ArrayList<>();
    private final List<TreeBalance> balances = new ArrayList<>();

    private YieldForecastService yieldForecastService() {
        return new YieldForecastService(populationEventRepository, varietyReferenceRepository,
                new GrowthPhaseYieldShareService(growthPhaseYieldShareRepository));
    }

    private static Clock noonOf(LocalDate day) {
        return Clock.fixed(day.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    }

    /** The service on a clock fixed at noon UTC of a day. */
    private HarvestStatusService serviceOn(LocalDate today) {
        return new HarvestStatusService(yieldForecastService(), healthTreatmentService, harvestService,
                noonOf(today));
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

    /** A variety row of farm 7 planted on a day; the plantings are stubbed by {@link #stubPlantings}. */
    private Variety planted(long id, String blockCode, String name, LocalDate plantedOn, int trees) {
        Variety variety = new Variety();
        variety.setId(id);
        variety.setFarmId(FARM);
        variety.setBlockCode(blockCode);
        variety.setName(name);
        plantings.add(new PopulationEvent(id + 100, variety, PopulationEventType.PLANTING, plantedOn, trees,
                "user_entry", null));
        balances.add(balance(id, trees));
        return variety;
    }

    /** Stubs the plantings of farm 7 and the Keitt reference of the dev loader, May to July. */
    private void stubPlantings() {
        when(populationEventRepository.findPlantingsByOptionalFilters(FARM, null)).thenReturn(plantings);
        when(populationEventRepository.findTreeBalances(anyCollection())).thenReturn(balances);
        when(varietyReferenceRepository.findAll()).thenReturn(List.of(
                new VarietyReference(1L, "Keitt", 220.0, "Zalka_2025", 5, 7, KEITT_SEASON_SOURCE, null)));
    }

    private static HealthTreatmentResponse treatment(long id, String blockCode, LocalDate treatedOn,
                                                     int intervalDays) {
        return new HealthTreatmentResponse(id, FARM, blockCode, "OTHER", "Other", "Fruit fly", null,
                treatedOn, "Insecticide B", "deltamethrin", 1.5, TreatmentUnit.L, intervalDays,
                treatedOn.plusDays(intervalDays), "Awa", null, "user_entry", null);
    }

    private static HarvestResponse harvest(long id, Variety variety, LocalDate harvestDate) {
        return new HarvestResponse(id, FARM, variety.getBlockCode(), variety.getId(), variety.getName(),
                harvestDate, 100.0, "user_entry", null);
    }

    /** The status of a Keitt row with its harvest window, the default share of gradual production. */
    private static VarietyHarvestStatus keitt(Variety variety, int trees, MaturityStatus status,
                                              LocalDate windowStart, LocalDate windowEnd, Integer days,
                                              HealthTreatmentResponse interval, LocalDate firstHarvestDate) {
        return new VarietyHarvestStatus(FARM, variety.getBlockCode(), variety.getId(), variety.getName(), trees,
                status, windowStart, windowEnd, days,
                interval == null ? null : interval.harvestAllowedFrom(), interval == null ? null : interval.id(),
                firstHarvestDate, KEITT_SEASON_SOURCE, windowStart == null ? null : "Bally_2002");
    }

    @Test
    void getHarvestStatus_shouldBeReadyForHarvest_whenTheWindowIsOpenAndNoIntervalRuns() {
        // Arrange: Keitt on A, 3 years old in May 2027; a harvest in April, before the window, is not read
        Variety keittA = planted(1L, "A", "Keitt", KEITT_A_PLANTED_ON, 40);
        stubPlantings();
        when(harvestService.getAllHarvests(FARM, null, LocalDate.of(2027, 5, 1), JUNE_15_2027)).thenReturn(List.of(
                harvest(11L, keittA, LocalDate.of(2027, 5, 20)), harvest(12L, keittA, LocalDate.of(2027, 6, 10))));
        // Act
        HarvestStatusResponse response = serviceOn(JUNE_15_2027).getHarvestStatus(FARM, null, null);
        // Assert
        assertEquals(JUNE_15_2027, response.today());
        assertEquals(30, response.withinDays());
        assertEquals(List.of(keitt(keittA, 40, MaturityStatus.READY_FOR_HARVEST, LocalDate.of(2027, 5, 1),
                LocalDate.of(2027, 7, 31), -45, null, LocalDate.of(2027, 5, 20))), response.varieties());
    }

    @Test
    void getHarvestStatus_shouldBeRipe_whileAPreHarvestIntervalRunsOnTheBlock() {
        // Arrange: of the treatments of block A, the one whose interval ends last counts; the others do not
        Variety keittA = planted(1L, "A", "Keitt", KEITT_A_PLANTED_ON, 40);
        stubPlantings();
        HealthTreatmentResponse lastToEnd = treatment(21L, "A", LocalDate.of(2027, 6, 10), 14);
        when(healthTreatmentService.getAllTreatments(FARM, null, null, JUNE_15_2027)).thenReturn(List.of(
                treatment(20L, "A", LocalDate.of(2027, 5, 1), 14),
                lastToEnd,
                treatment(22L, "A", LocalDate.of(2027, 6, 12), 7),
                treatment(23L, "B", LocalDate.of(2027, 6, 14), 21)));
        // Act
        HarvestStatusResponse response = serviceOn(JUNE_15_2027).getHarvestStatus(FARM, null, null);
        // Assert: harvest allowed from 24 June 2027
        assertEquals(List.of(keitt(keittA, 40, MaturityStatus.RIPE, LocalDate.of(2027, 5, 1),
                LocalDate.of(2027, 7, 31), -45, lastToEnd, null)), response.varieties());
        assertEquals(LocalDate.of(2027, 6, 24), response.varieties().get(0).harvestAllowedFrom());
    }

    @Test
    void getHarvestStatus_shouldBeRipe_withinTheLookAheadOfTheAlerts() {
        // Arrange
        Variety keittA = planted(1L, "A", "Keitt", KEITT_A_PLANTED_ON, 40);
        stubPlantings();
        LocalDate april10 = LocalDate.of(2027, 4, 10);
        // Act & Assert: the window opens in 21 days, ripe within 30 days but not within 20
        assertEquals(List.of(keitt(keittA, 40, MaturityStatus.RIPE, LocalDate.of(2027, 5, 1),
                        LocalDate.of(2027, 7, 31), 21, null, null)),
                serviceOn(april10).getHarvestStatus(FARM, null, null).varieties());
        HarvestStatusResponse within20 = serviceOn(april10).getHarvestStatus(FARM, null, 20);
        assertEquals(20, within20.withinDays());
        assertEquals(MaturityStatus.IMMATURE, within20.varieties().get(0).maturityStatus());
        // ... and immature 47 days before
        assertEquals(MaturityStatus.IMMATURE,
                serviceOn(LocalDate.of(2027, 3, 15)).getHarvestStatus(FARM, null, null)
                        .varieties().get(0).maturityStatus());
        verifyNoInteractions(harvestService);
    }

    @Test
    void getHarvestStatus_shouldEstimateTheFirstHarvest_yearsAhead_forYoungTrees() {
        // Arrange: Keitt on B planted in March 2026, in establishment in 2027 and 2028
        Variety keittB = planted(2L, "B", "Keitt", LocalDate.of(2026, 3, 1), 100);
        stubPlantings();
        // Act
        HarvestStatusResponse response = serviceOn(JUNE_15_2027).getHarvestStatus(FARM, null, null);
        // Assert: 3 years old on 1 May 2029, 686 days ahead
        assertEquals(List.of(keitt(keittB, 100, MaturityStatus.IMMATURE, LocalDate.of(2029, 5, 1),
                LocalDate.of(2029, 7, 31), 686, null, null)), response.varieties());
        verifyNoInteractions(harvestService);
    }

    @Test
    void getHarvestStatus_shouldGiveNoDate_whenNoPhaseGivesAYield() {
        // Arrange: every share corrected to 0
        Variety keittA = planted(1L, "A", "Keitt", KEITT_A_PLANTED_ON, 40);
        stubPlantings();
        when(growthPhaseYieldShareRepository.findAll()).thenReturn(List.of(
                new GrowthPhaseYieldShare(1L, "establishment", 0.0, "user_entry", null),
                new GrowthPhaseYieldShare(2L, "gradual production", 0.0, "user_entry", null),
                new GrowthPhaseYieldShare(3L, "full production", 0.0, "user_entry", null)));
        // Act
        HarvestStatusResponse response = serviceOn(JUNE_15_2027).getHarvestStatus(FARM, null, null);
        // Assert: the search ends once the trees are in full production
        assertEquals(List.of(keitt(keittA, 40, MaturityStatus.IMMATURE, null, null, null, null, null)),
                response.varieties());
    }

    @Test
    void getHarvestStatus_shouldBeImmature_afterTheSeason_untilTheNextOneComesNear() {
        // Arrange
        Variety keittA = planted(1L, "A", "Keitt", KEITT_A_PLANTED_ON, 40);
        stubPlantings();
        // Act & Assert: on 1 August 2027 the next window opens in 274 days
        assertEquals(List.of(keitt(keittA, 40, MaturityStatus.IMMATURE, LocalDate.of(2028, 5, 1),
                        LocalDate.of(2028, 7, 31), 274, null, null)),
                serviceOn(LocalDate.of(2027, 8, 1)).getHarvestStatus(FARM, null, null).varieties());
    }

    @Test
    void getHarvestStatus_shouldGiveNoStatus_withoutReference_butStillTheInterval() {
        // Arrange: Brooks is not in the reference; listed after Keitt on A by block
        Variety keittA = planted(1L, "A", "Keitt", KEITT_A_PLANTED_ON, 40);
        Variety brooks = planted(3L, "C", "Brooks", LocalDate.of(2020, 1, 10), 30);
        stubPlantings();
        HealthTreatmentResponse onC = treatment(24L, "C", LocalDate.of(2027, 6, 14), 7);
        when(healthTreatmentService.getAllTreatments(FARM, null, null, JUNE_15_2027)).thenReturn(List.of(onC));
        // Act
        HarvestStatusResponse response = serviceOn(JUNE_15_2027).getHarvestStatus(FARM, "  ", null);
        // Assert
        assertEquals(List.of(
                keitt(keittA, 40, MaturityStatus.READY_FOR_HARVEST, LocalDate.of(2027, 5, 1),
                        LocalDate.of(2027, 7, 31), -45, null, null),
                new VarietyHarvestStatus(FARM, "C", brooks.getId(), "Brooks", 30, null, null, null, null,
                        LocalDate.of(2027, 6, 21), 24L, null, null, null)), response.varieties());
    }

    @Test
    void maturityStatus_shouldAgreeWithTheAlerts_everyDayOfTheSeason() {
        // Arrange: Keitt on A, young Keitt on B, and a treatment on A in the season
        planted(1L, "A", "Keitt", KEITT_A_PLANTED_ON, 40);
        planted(2L, "B", "Keitt", LocalDate.of(2026, 3, 1), 100);
        stubPlantings();
        HealthTreatmentResponse onA = treatment(21L, "A", LocalDate.of(2027, 6, 10), 14);
        when(healthTreatmentService.getAllTreatments(eq(FARM), isNull(), isNull(), any(LocalDate.class)))
                .thenAnswer(invocation -> {
                    LocalDate to = invocation.getArgument(3);
                    return onA.treatedOn().isAfter(to) ? List.of() : List.of(onA);
                });
        // Act & Assert: READY with an alert in progress and no interval, RIPE with an alert to come or a
        // critical interval, IMMATURE without harvest alert
        for (LocalDate day = LocalDate.of(2027, 3, 1); day.isBefore(LocalDate.of(2027, 8, 16));
             day = day.plusDays(1)) {
            List<PlantAlert> alerts = new PlantAlertService(yieldForecastService(), healthTreatmentService,
                    healthFindingService, fertilizerService, nurseryBatchService, noonOf(day))
                    .getAlerts(FARM, null, null).alerts();
            for (VarietyHarvestStatus status : serviceOn(day).getHarvestStatus(FARM, null, null).varieties()) {
                List<PlantAlert> harvestAlerts = alerts.stream()
                        .filter(alert -> alert.type() == PlantAlertType.HARVEST_APPROACHING
                                && status.varietyId().equals(alert.varietyId()))
                        .toList();
                boolean critical = alerts.stream()
                        .anyMatch(alert -> alert.type() == PlantAlertType.PRE_HARVEST_INTERVAL
                                && alert.severity() == PlantAlertSeverity.CRITICAL
                                && status.blockCode().equals(alert.blockCode()));
                boolean inProgress = harvestAlerts.stream().anyMatch(alert -> alert.daysFromToday() <= 0);
                boolean toCome = harvestAlerts.stream().anyMatch(alert -> alert.daysFromToday() > 0);
                MaturityStatus expected = inProgress && !critical
                        ? MaturityStatus.READY_FOR_HARVEST
                        : toCome || critical ? MaturityStatus.RIPE : MaturityStatus.IMMATURE;
                assertEquals(expected, status.maturityStatus(), day + " " + status.blockCode());
            }
        }
    }
}
