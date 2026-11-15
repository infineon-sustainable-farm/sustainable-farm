package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HarvestStatusResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HarvestStatusResponse.VarietyHarvestStatus;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantAlertResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantAlertResponse.PlantAlert;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantsOverviewResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantsOverviewResponse.MaturityShare;
import com.infineonbit.sustainablefarm.modules.plants.entity.MaturityStatus;
import com.infineonbit.sustainablefarm.modules.plants.entity.PlantAlertSeverity;
import com.infineonbit.sustainablefarm.modules.plants.entity.PlantAlertType;
import com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEvent;
import com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEventType;
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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The tree figures are computed from the real forecast basis, on mocked
 * repositories; the harvest status and the alerts, tested on their own, are
 * mocked: only the sums are tested here.
 */
@ExtendWith(MockitoExtension.class)
public class PlantsOverviewServiceTest {

    private static final int FARM = 7;
    private static final LocalDate JUNE_15_2027 = LocalDate.of(2027, 6, 15);

    /** The trees by maturity status when nothing is planted: every entry, with 0 trees and no share. */
    private static final List<MaturityShare> NO_TREE = List.of(
            new MaturityShare(MaturityStatus.IMMATURE, 0, null),
            new MaturityShare(MaturityStatus.RIPE, 0, null),
            new MaturityShare(MaturityStatus.READY_FOR_HARVEST, 0, null),
            new MaturityShare(null, 0, null));

    @Mock
    private PopulationEventRepository populationEventRepository;

    @Mock
    private VarietyReferenceRepository varietyReferenceRepository;

    @Mock
    private GrowthPhaseYieldShareRepository growthPhaseYieldShareRepository;

    @Mock
    private HarvestStatusService harvestStatusService;

    @Mock
    private PlantAlertService plantAlertService;

    private final List<PopulationEvent> plantings = new ArrayList<>();
    private final List<TreeBalance> balances = new ArrayList<>();

    /** The service on a clock fixed at noon UTC of a day. */
    private PlantsOverviewService serviceOn(LocalDate today) {
        Clock clock = Clock.fixed(today.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        return new PlantsOverviewService(new YieldForecastService(populationEventRepository,
                varietyReferenceRepository, new GrowthPhaseYieldShareService(growthPhaseYieldShareRepository)),
                harvestStatusService, plantAlertService, clock);
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

    /** A variety row of farm 7 planted on a day, with trees left; stubbed by {@link #stubPlantings}. */
    private void planted(long id, String blockCode, String name, LocalDate plantedOn, long treesLeft) {
        Variety variety = new Variety();
        variety.setId(id);
        variety.setFarmId(FARM);
        variety.setBlockCode(blockCode);
        variety.setName(name);
        plantings.add(new PopulationEvent(id + 100, variety, PopulationEventType.PLANTING, plantedOn,
                (int) Math.max(treesLeft, 1), "user_entry", null));
        balances.add(balance(id, treesLeft));
    }

    private void stubPlantings(String blockCode) {
        when(populationEventRepository.findPlantingsByOptionalFilters(FARM, blockCode)).thenReturn(plantings);
        when(populationEventRepository.findTreeBalances(anyCollection())).thenReturn(balances);
        when(varietyReferenceRepository.findAll()).thenReturn(List.of(
                new VarietyReference(1L, "Keitt", 220.0, "Zalka_2025", 5, 7, "varietal_guide_west_africa", null)));
    }

    /** A line whose status holds as given, from the estimate; the overview reads no other field. */
    private static VarietyHarvestStatus status(long varietyId, int trees, MaturityStatus maturityStatus) {
        return new VarietyHarvestStatus(FARM, "A", varietyId, "Keitt", trees, maturityStatus,
                maturityStatus == null ? null : "estimate", maturityStatus, null,
                null, null, null, null, null, null, null, null);
    }

    private static PlantAlert alert(PlantAlertType type, PlantAlertSeverity severity) {
        return new PlantAlert(type, severity, FARM, "A", null, "An alert", null, null, null,
                null, null, null, null, null, null);
    }

    private void statusesAndAlerts(String blockCode, LocalDate today, List<VarietyHarvestStatus> statuses,
                                   List<PlantAlert> alerts) {
        when(harvestStatusService.getHarvestStatus(FARM, blockCode, null, today))
                .thenReturn(new HarvestStatusResponse(today, 30, statuses));
        when(plantAlertService.getAlerts(FARM, blockCode, null, today))
                .thenReturn(new PlantAlertResponse(today, 30, alerts, List.of()));
    }

    @Test
    void getOverview_shouldSumUpThePlantedTrees_theStatusesAndTheAlertsOfTheDay() {
        // Arrange: Keitt on A in gradual production, young Keitt on B, Brooks on C without reference
        planted(1L, "A", "Keitt", LocalDate.of(2023, 9, 15), 40);
        planted(2L, "B", "Keitt", LocalDate.of(2026, 3, 1), 100);
        planted(3L, "C", "Brooks", LocalDate.of(2020, 1, 10), 30);
        stubPlantings(null);
        statusesAndAlerts(null, JUNE_15_2027,
                List.of(status(1L, 40, MaturityStatus.READY_FOR_HARVEST), status(2L, 100, MaturityStatus.IMMATURE),
                        status(3L, 30, null)),
                List.of(alert(PlantAlertType.PRE_HARVEST_INTERVAL, PlantAlertSeverity.CRITICAL),
                        alert(PlantAlertType.HARVEST_APPROACHING, PlantAlertSeverity.WARNING),
                        alert(PlantAlertType.LOW_STOCK, PlantAlertSeverity.WARNING)));
        // Act
        PlantsOverviewResponse overview = serviceOn(JUNE_15_2027).getOverview(FARM, null);
        // Assert: the trees of Brooks count, without reference; RIPE is listed with no tree
        assertEquals(new PlantsOverviewResponse(JUNE_15_2027, 170, 100, 40, 3, 1, List.of(
                new MaturityShare(MaturityStatus.IMMATURE, 100, 58.8),
                new MaturityShare(MaturityStatus.RIPE, 0, 0.0),
                new MaturityShare(MaturityStatus.READY_FOR_HARVEST, 40, 23.5),
                new MaturityShare(null, 30, 17.6))), overview);
    }

    @Test
    void getOverview_shouldCountTheGrowthStage_upToTheThirdBirthdayOfTheTrees() {
        // Arrange: 3 years old today, one day short of it, planted today; and a row with no tree left
        planted(1L, "A", "Keitt", LocalDate.of(2024, 6, 15), 10);
        planted(2L, "A", "Kent", LocalDate.of(2024, 6, 16), 20);
        planted(3L, "A", "Amelie", JUNE_15_2027, 30);
        planted(4L, "A", "Palmer", LocalDate.of(2026, 1, 1), 0);
        stubPlantings("A");
        statusesAndAlerts("A", JUNE_15_2027, List.of(status(1L, 10, MaturityStatus.IMMATURE), status(2L, 20, null),
                status(3L, 30, null)), List.of());
        // Act
        PlantsOverviewResponse overview = serviceOn(JUNE_15_2027).getOverview(FARM, "A");
        // Assert: Kent and Amelie are not in the reference
        assertEquals(new PlantsOverviewResponse(JUNE_15_2027, 60, 50, 0, 0, 0, List.of(
                new MaturityShare(MaturityStatus.IMMATURE, 10, 16.7),
                new MaturityShare(MaturityStatus.RIPE, 0, 0.0),
                new MaturityShare(MaturityStatus.READY_FOR_HARVEST, 0, 0.0),
                new MaturityShare(null, 50, 83.3))), overview);
        verify(plantAlertService).getAlerts(FARM, "A", null, JUNE_15_2027);
    }

    @Test
    void getOverview_shouldGiveZeros_whenNothingIsPlanted() {
        // Arrange
        when(populationEventRepository.findPlantingsByOptionalFilters(FARM, null)).thenReturn(List.of());
        when(varietyReferenceRepository.findAll()).thenReturn(List.of());
        statusesAndAlerts(null, JUNE_15_2027, List.of(), List.of());
        // Act & Assert
        assertEquals(new PlantsOverviewResponse(JUNE_15_2027, 0, 0, 0, 0, 0, NO_TREE),
                serviceOn(JUNE_15_2027).getOverview(FARM, null));
    }

    @Test
    void getOverview_shouldRoundEachShareHalfUpOnItsOwn() {
        // Arrange: 15 trees of 16 are 93.75%, 1 is 6.25%
        planted(1L, "A", "Keitt", LocalDate.of(2015, 6, 1), 15);
        planted(2L, "B", "Keitt", LocalDate.of(2015, 6, 1), 1);
        stubPlantings(null);
        statusesAndAlerts(null, JUNE_15_2027,
                List.of(status(1L, 15, MaturityStatus.IMMATURE), status(2L, 1, MaturityStatus.READY_FOR_HARVEST)),
                List.of());
        // Act
        PlantsOverviewResponse overview = serviceOn(JUNE_15_2027).getOverview(FARM, null);
        // Assert: both halves go up, so the shares make 100.1
        assertEquals(List.of(
                new MaturityShare(MaturityStatus.IMMATURE, 15, 93.8),
                new MaturityShare(MaturityStatus.RIPE, 0, 0.0),
                new MaturityShare(MaturityStatus.READY_FOR_HARVEST, 1, 6.3),
                new MaturityShare(null, 0, 0.0)), overview.maturity());
    }
}
