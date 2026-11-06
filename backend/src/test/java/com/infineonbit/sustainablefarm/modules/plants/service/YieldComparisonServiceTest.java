package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HarvestResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldComparisonResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldComparisonResponse.BlockComparison;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldComparisonResponse.Comparison;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldComparisonResponse.NotCompared;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldComparisonResponse.PhaseShare;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldComparisonResponse.VarietyComparison;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldForecastResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldForecastResponse.Entry;
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
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The comparison is computed from the real forecast, on mocked repositories,
 * and from the harvests of the harvest service, mocked. The growth phases keep
 * their default shares: 0 in establishment, 0.25 in gradual production (Bally
 * 2002), 1 in full production. Every line is compared in full.
 */
@ExtendWith(MockitoExtension.class)
public class YieldComparisonServiceTest {

    private static final int FARM = 7;
    private static final LocalDate JUNE_15_2027 = LocalDate.of(2027, 6, 15);
    private static final String KEITT_SEASON_SOURCE = "varietal_guide_west_africa";
    private static final PhaseShare GRADUAL = new PhaseShare("gradual production", 0.25, "Bally_2002");
    private static final PhaseShare ESTABLISHMENT = new PhaseShare("establishment", 0.0, "orchard_literature");

    @Mock
    private PopulationEventRepository populationEventRepository;

    @Mock
    private VarietyReferenceRepository varietyReferenceRepository;

    @Mock
    private GrowthPhaseYieldShareRepository growthPhaseYieldShareRepository;

    @Mock
    private HarvestService harvestService;

    private final List<PopulationEvent> plantings = new ArrayList<>();
    private final List<TreeBalance> balances = new ArrayList<>();

    /** The real forecast and shares on the mocked repositories: the comparison follows their rules. */
    private YieldForecastService yieldForecastService() {
        return new YieldForecastService(populationEventRepository, varietyReferenceRepository,
                new GrowthPhaseYieldShareService(growthPhaseYieldShareRepository));
    }

    /** The service on a clock fixed at noon UTC of 15 June 2027. */
    private YieldComparisonService service() {
        Clock clock = Clock.fixed(JUNE_15_2027.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        return new YieldComparisonService(yieldForecastService(), harvestService, clock);
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

    /** A variety row of farm 7 planted on a day, with trees left; the plantings are stubbed by {@link #stubPlantings}. */
    private Variety planted(long id, String blockCode, String name, LocalDate plantedOn, long treesLeft) {
        Variety variety = new Variety();
        variety.setId(id);
        variety.setFarmId(FARM);
        variety.setBlockCode(blockCode);
        variety.setName(name);
        plantings.add(new PopulationEvent(id + 100, variety, PopulationEventType.PLANTING, plantedOn,
                (int) treesLeft, "user_entry", null));
        balances.add(balance(id, treesLeft));
        return variety;
    }

    /** Stubs the plantings of farm 7 and the Keitt and Kent references of the dev loader. */
    private void stubPlantings() {
        when(populationEventRepository.findPlantingsByOptionalFilters(FARM, null)).thenReturn(plantings);
        when(populationEventRepository.findTreeBalances(anyCollection())).thenReturn(balances);
        when(varietyReferenceRepository.findAll()).thenReturn(List.of(
                new VarietyReference(1L, "Keitt", 220.0, "Zalka_2025", 5, 7, KEITT_SEASON_SOURCE, null),
                new VarietyReference(2L, "Kent", 200.0, "Zalka_2025", 4, 5, "FAO_mango_burkina", null)));
    }

    private static HarvestResponse harvest(long id, Variety variety, LocalDate harvestDate, double quantityKg) {
        return new HarvestResponse(id, null, variety.getFarmId(), variety.getBlockCode(), variety.getId(),
                variety.getName(), harvestDate, quantityKg, "user_entry", null);
    }

    /** Stubs the harvests of farm 7 in 2027, the default months on 15 June 2027. */
    private void harvestsOf2027(HarvestResponse... harvests) {
        when(harvestService.getAllHarvests(FARM, null, LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31)))
                .thenReturn(List.of(harvests));
    }

    private static VarietyComparison keitt(Variety variety, int trees, Double expectedKg, Double actualKg,
                                           int harvestCount, Double achievedPct, List<PhaseShare> phases) {
        return new VarietyComparison(FARM, variety.getBlockCode(), variety.getId(), variety.getName(), trees,
                expectedKg, actualKg, harvestCount, achievedPct, 220.0, "Zalka_2025", KEITT_SEASON_SOURCE, phases);
    }

    @Test
    void getYieldComparison_shouldCompareTheHarvestsWithTheForecastOfTheCalendarYear_byDefault() {
        // Arrange: Keitt on A in gradual production in May-July 2027, Keitt on B still in establishment
        Variety keittA = planted(1L, "A", "Keitt", LocalDate.of(2023, 9, 15), 40);
        Variety keittB = planted(2L, "B", "Keitt", LocalDate.of(2026, 3, 1), 100);
        stubPlantings();
        harvestsOf2027(harvest(11L, keittA, LocalDate.of(2027, 5, 20), 300.0),
                harvest(12L, keittA, LocalDate.of(2027, 6, 10), 250.5));
        // Act
        YieldComparisonResponse comparison = service().getYieldComparison(FARM, null, null, null);
        // Assert: three months of 40 × 220 × 0.25 / 3 = 733.3 kg, added up as rounded
        assertEquals(YearMonth.of(2027, 1), comparison.from());
        assertEquals(12, comparison.months());
        assertEquals("recorded_plantings", comparison.basis());
        assertEquals(List.of(
                keitt(keittA, 40, 2199.9, 550.5, 2, 25.0, List.of(GRADUAL)),
                keitt(keittB, 100, 0.0, null, 0, null, List.of(ESTABLISHMENT))), comparison.varieties());
        assertEquals(List.of(
                new BlockComparison(FARM, "A", 2199.9, 550.5, 25.0),
                new BlockComparison(FARM, "B", 0.0, null, null)), comparison.blocks());
        assertEquals(new Comparison(2199.9, 550.5, 25.0), comparison.total());
        assertEquals(List.of(), comparison.notCompared());
    }

    @Test
    void getYieldComparison_shouldExpectWhatTheForecastExpects_overTheSameMonths() {
        // Arrange: a phase change inside the season, Keitt 3 years old on 1 July 2027 only
        Variety keitt = planted(1L, "A", "Keitt", LocalDate.of(2024, 6, 20), 40);
        stubPlantings();
        when(harvestService.getAllHarvests(FARM, null, LocalDate.of(2027, 5, 1), LocalDate.of(2027, 7, 31)))
                .thenReturn(List.of());
        // Act
        YieldComparisonResponse comparison = service().getYieldComparison(FARM, null, YearMonth.of(2027, 5), 3);
        YieldForecastResponse forecast = yieldForecastService()
                .getYieldForecast(FARM, null, YearMonth.of(2027, 5), 3, JUNE_15_2027);
        // Assert: July only, 733.3 kg, as in the forecast; both phases of the harvest months are shown
        assertEquals(List.of(733.3), forecast.entries().stream().map(Entry::expectedKg).toList());
        assertEquals(List.of(keitt(keitt, 40, 733.3, null, 0, null, List.of(ESTABLISHMENT, GRADUAL))),
                comparison.varieties());
        assertEquals(YearMonth.of(2027, 5), comparison.from());
        assertEquals(3, comparison.months());
    }

    @Test
    void getYieldComparison_shouldAddTheHarvestsExactly_thenRoundToATenth() {
        // Arrange: 0.1 + 0.2 as doubles is 0.30000000000000004; 12.25 + 3.333 is 15.583
        Variety keittA = planted(1L, "A", "Keitt", LocalDate.of(2023, 9, 15), 40);
        Variety keittC = planted(3L, "C", "Keitt", LocalDate.of(2023, 9, 15), 40);
        stubPlantings();
        harvestsOf2027(harvest(11L, keittA, LocalDate.of(2027, 5, 20), 0.1),
                harvest(12L, keittA, LocalDate.of(2027, 5, 21), 0.2),
                harvest(13L, keittC, LocalDate.of(2027, 5, 20), 12.25),
                harvest(14L, keittC, LocalDate.of(2027, 5, 21), 3.333));
        // Act
        List<VarietyComparison> lines = service().getYieldComparison(FARM, null, null, null).varieties();
        // Assert
        assertEquals(0.3, lines.get(0).actualKg());
        assertEquals(0.0, lines.get(0).achievedPct());
        assertEquals(15.6, lines.get(1).actualKg());
        assertEquals(0.7, lines.get(1).achievedPct());
    }

    @Test
    void getYieldComparison_shouldGiveNoPercentage_whenNothingIsExpected() {
        // Arrange: a few fruits picked on trees in establishment
        Variety keittB = planted(2L, "B", "Keitt", LocalDate.of(2026, 3, 1), 100);
        stubPlantings();
        harvestsOf2027(harvest(11L, keittB, LocalDate.of(2027, 6, 1), 12.0));
        // Act
        YieldComparisonResponse comparison = service().getYieldComparison(FARM, null, null, null);
        // Assert
        assertEquals(List.of(keitt(keittB, 100, 0.0, 12.0, 1, null, List.of(ESTABLISHMENT))),
                comparison.varieties());
        assertEquals(new Comparison(0.0, 12.0, null), comparison.total());
    }

    @Test
    void getYieldComparison_shouldAddUpTheVarietiesOfABlock() {
        // Arrange: Keitt and Kent on block A, both in gradual production
        Variety keitt = planted(1L, "A", "Keitt", LocalDate.of(2023, 9, 15), 40);
        Variety kent = planted(4L, "A", "Kent", LocalDate.of(2023, 9, 15), 30);
        stubPlantings();
        harvestsOf2027(harvest(11L, kent, LocalDate.of(2027, 4, 20), 1000.0));
        // Act
        YieldComparisonResponse comparison = service().getYieldComparison(FARM, null, null, null);
        // Assert: Kent, 30 × 200 × 0.25 / 2 = 750 kg in April and in May
        assertEquals(List.of(1L, 4L), comparison.varieties().stream().map(VarietyComparison::varietyId).toList());
        assertEquals(new VarietyComparison(FARM, "A", kent.getId(), "Kent", 30, 1500.0, 1000.0, 1, 66.7,
                200.0, "Zalka_2025", "FAO_mango_burkina", List.of(GRADUAL)), comparison.varieties().get(1));
        assertEquals(List.of(new BlockComparison(FARM, "A", 3699.9, 1000.0, 27.0)), comparison.blocks());
        assertEquals(new Comparison(3699.9, 1000.0, 27.0), comparison.total());
        assertEquals(keitt.getId(), comparison.varieties().get(0).varietyId());
    }

    @Test
    void getYieldComparison_shouldListTheVarietiesWithoutReferenceApart_withTheirHarvests() {
        // Arrange: Brooks and Palmer are not in the reference
        Variety keitt = planted(1L, "A", "Keitt", LocalDate.of(2023, 9, 15), 40);
        Variety brooks = planted(3L, "C", "Brooks", LocalDate.of(2020, 1, 10), 30);
        planted(5L, "D", "Palmer", LocalDate.of(2021, 2, 1), 10);
        stubPlantings();
        harvestsOf2027(harvest(11L, brooks, LocalDate.of(2027, 6, 5), 120.0));
        // Act
        YieldComparisonResponse comparison = service().getYieldComparison(FARM, null, null, null);
        // Assert: their harvests count nowhere else
        assertEquals(List.of(keitt.getId()),
                comparison.varieties().stream().map(VarietyComparison::varietyId).toList());
        assertEquals(new Comparison(2199.9, null, null), comparison.total());
        assertEquals(List.of(
                new NotCompared(FARM, "C", 3L, "Brooks", 30, 120.0, 1, "NO_REFERENCE"),
                new NotCompared(FARM, "D", 5L, "Palmer", 10, null, 0, "NO_REFERENCE")), comparison.notCompared());
    }

    @Test
    void getYieldComparison_shouldListTheHarvestsOfARowWithNoTreeLeft() {
        // Arrange: Keitt on E lost all its trees after a harvest of 2027
        Variety keittE = planted(6L, "E", "Keitt", LocalDate.of(2023, 9, 15), 0);
        stubPlantings();
        harvestsOf2027(harvest(11L, keittE, LocalDate.of(2027, 5, 20), 80.0),
                harvest(12L, keittE, LocalDate.of(2027, 5, 27), 40.0));
        // Act
        YieldComparisonResponse comparison = service().getYieldComparison(FARM, null, null, null);
        // Assert
        assertEquals(List.of(), comparison.varieties());
        assertEquals(List.of(new NotCompared(FARM, "E", 6L, "Keitt", null, 120.0, 2, "NO_TREE_LEFT")),
                comparison.notCompared());
    }

    @Test
    void getYieldComparison_shouldLeaveOutTheVarietiesPlantedAfterTheMonths() {
        // Arrange: Keitt and Brooks planted in January 2028
        planted(1L, "A", "Keitt", LocalDate.of(2028, 1, 10), 40);
        planted(3L, "C", "Brooks", LocalDate.of(2028, 1, 10), 30);
        stubPlantings();
        harvestsOf2027();
        // Act
        YieldComparisonResponse comparison = service().getYieldComparison(FARM, null, null, null);
        // Assert
        assertEquals(List.of(), comparison.varieties());
        assertEquals(List.of(), comparison.blocks());
        assertEquals(new Comparison(0.0, null, null), comparison.total());
        assertEquals(List.of(), comparison.notCompared());
    }

    @Test
    void getYieldComparison_shouldPassTheFiltersAndTheMonthsToTheHarvests() {
        // Arrange
        when(populationEventRepository.findPlantingsByOptionalFilters(null, "B")).thenReturn(List.of());
        when(varietyReferenceRepository.findAll()).thenReturn(List.of());
        when(harvestService.getAllHarvests(null, "B", LocalDate.of(2026, 11, 1), LocalDate.of(2027, 2, 28)))
                .thenReturn(List.of());
        // Act
        YieldComparisonResponse comparison = service().getYieldComparison(null, "B", YearMonth.of(2026, 11), 4);
        // Assert: a season over the new year is compared in one piece this way
        assertEquals(YearMonth.of(2026, 11), comparison.from());
        assertEquals(4, comparison.months());
        verify(harvestService).getAllHarvests(null, "B", LocalDate.of(2026, 11, 1), LocalDate.of(2027, 2, 28));
    }
}
