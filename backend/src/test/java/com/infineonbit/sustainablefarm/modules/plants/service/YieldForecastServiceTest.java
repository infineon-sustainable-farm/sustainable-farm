package com.infineonbit.sustainablefarm.modules.plants.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldForecastResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldForecastResponse.Entry;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldForecastResponse.MonthlyTotal;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldForecastResponse.VarietyWithoutReference;
import com.infineonbit.sustainablefarm.modules.plants.entity.GrowthPhaseYieldShare;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class YieldForecastServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 24);
    private static final YearMonth JANUARY_2027 = YearMonth.of(2027, 1);

    @Mock
    private PopulationEventRepository populationEventRepository;

    @Mock
    private VarietyReferenceRepository varietyReferenceRepository;

    @Mock
    private GrowthPhaseYieldShareRepository growthPhaseYieldShareRepository;

    @InjectMocks
    private YieldForecastService yieldForecastService;

    private static Variety variety(long id, Integer farmId, String blockCode, String name) {
        Variety variety = new Variety();
        variety.setId(id);
        variety.setFarmId(farmId);
        variety.setBlockCode(blockCode);
        variety.setName(name);
        return variety;
    }

    private static PopulationEvent planting(Variety variety, LocalDate date, int treeCount) {
        return new PopulationEvent(
                variety.getId() + 100, variety, PopulationEventType.PLANTING, date, treeCount, "user_entry", null);
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

    private static VarietyReference reference(long id, String name, double yieldPerTreeKg,
                                              int harvestStartMonth, int harvestEndMonth, String seasonSource) {
        return new VarietyReference(id, name, yieldPerTreeKg, "Zalka_2025",
                harvestStartMonth, harvestEndMonth, seasonSource, null);
    }

    /** The defaults of AgronomicReferenceLoader, as they sit in the tables. */
    private static List<VarietyReference> defaultReferences() {
        return List.of(
                reference(1L, "Keitt", 220.0, 5, 7, "varietal_guide_west_africa"),
                reference(2L, "Kent", 200.0, 4, 5, "FAO_mango_burkina"),
                reference(3L, "Amelie", 160.0, 2, 4, "FAO_mango_burkina"));
    }

    private static List<GrowthPhaseYieldShare> shares(double establishment, double gradual, double full) {
        return List.of(
                new GrowthPhaseYieldShare(1L, "establishment", establishment, "orchard_literature", null),
                new GrowthPhaseYieldShare(2L, "gradual production", gradual, "assumption_to_validate", null),
                new GrowthPhaseYieldShare(3L, "full production", full, "by_definition", null));
    }

    /** Stubs the plantings and the balance of each, one balance per planting, in the same order. */
    private void plantings(Integer farmId, String blockCode, List<PopulationEvent> plantings, long... balances) {
        when(populationEventRepository.findPlantingsByOptionalFilters(farmId, blockCode)).thenReturn(plantings);
        TreeBalance[] rows = new TreeBalance[balances.length];
        for (int i = 0; i < balances.length; i++) {
            rows[i] = balance(plantings.get(i).getVariety().getId(), balances[i]);
        }
        when(populationEventRepository.findTreeBalances(anyCollection())).thenReturn(Arrays.asList(rows));
    }

    @Test
    void getYieldForecast_shouldGiveTheWorkedExample_for2027() {
        // Arrange: the orchard of the runtime check, in the repository order (block, then name)
        Variety amelie = variety(3L, null, "A", "Amélie");
        Variety kent = variety(2L, null, "A", "Kent");
        Variety palmer = variety(4L, null, "A", "Palmer");
        Variety keitt = variety(1L, null, "B", "Keitt");
        plantings(null, null, List.of(
                        planting(amelie, LocalDate.of(2025, 3, 1), 20),
                        planting(kent, LocalDate.of(2020, 6, 1), 40),
                        planting(palmer, LocalDate.of(2024, 1, 10), 10),
                        planting(keitt, LocalDate.of(2023, 9, 24), 150)),
                20, 40, 10, 150);
        when(varietyReferenceRepository.findAll()).thenReturn(defaultReferences());
        when(growthPhaseYieldShareRepository.findAll()).thenReturn(shares(0.0, 0.5, 1.0));
        // Act
        YieldForecastResponse forecast = yieldForecastService.getYieldForecast(null, null, JANUARY_2027, 12, TODAY);
        // Assert: the header
        assertEquals(JANUARY_2027, forecast.from());
        assertEquals(12, forecast.months());
        assertEquals("recorded_plantings", forecast.basis());
        // Assert: every month has a total, zeros included, in order
        assertEquals(YieldForecastCalculator.window(JANUARY_2027, 12),
                forecast.monthlyTotals().stream().map(MonthlyTotal::month).toList());
        assertEquals(List.of(0.0, 0.0, 0.0, 4000.0, 9500.0, 5500.0, 5500.0, 0.0, 0.0, 0.0, 0.0, 0.0),
                forecast.monthlyTotals().stream().map(MonthlyTotal::expectedKg).toList());
        assertEquals(24500.0, forecast.monthlyTotals().stream().mapToDouble(MonthlyTotal::expectedKg).sum());
        // Assert: Kent in April and May, Keitt from May to July; Amélie is still in establishment
        assertEquals(List.of("2027-04 Kent 4000.0", "2027-05 Kent 4000.0", "2027-05 Keitt 5500.0",
                        "2027-06 Keitt 5500.0", "2027-07 Keitt 5500.0"),
                forecast.entries().stream()
                        .map(entry -> entry.month() + " " + entry.varietyName() + " " + entry.expectedKg())
                        .toList());
        // Assert: every factor of one entry, with its sources
        assertEquals(new Entry(YearMonth.of(2027, 5), null, "B", 1L, "Keitt", 150, 3, "gradual production",
                        220.0, 0.5, 0.3333, 5500.0, "Zalka_2025", "varietal_guide_west_africa",
                        "assumption_to_validate"),
                forecast.entries().get(2));
        // Assert: Palmer is listed, not guessed; Amélie found "Amelie" despite the accent
        assertEquals(List.of(new VarietyWithoutReference(null, "A", "Palmer", 10)),
                forecast.varietiesWithoutReference());
    }

    @Test
    void getYieldForecast_shouldReadTheYieldFromTheReference() {
        // Arrange: a mentor lowered the Keitt yield to 80 kg in the table
        Variety keitt = variety(1L, null, "B", "Keitt");
        plantings(null, null, List.of(planting(keitt, LocalDate.of(2023, 9, 24), 150)), 150);
        when(varietyReferenceRepository.findAll())
                .thenReturn(List.of(reference(1L, "Keitt", 80.0, 5, 7, "orchard_literature")));
        when(growthPhaseYieldShareRepository.findAll()).thenReturn(shares(0.0, 0.5, 1.0));
        // Act
        YieldForecastResponse forecast = yieldForecastService.getYieldForecast(null, null, JANUARY_2027, 12, TODAY);
        // Assert: 150 × 80 × 0.5 / 3 each month of the season
        assertEquals(List.of(2000.0, 2000.0, 2000.0), forecast.entries().stream().map(Entry::expectedKg).toList());
    }

    @Test
    void getYieldForecast_shouldDefaultToTheCurrentMonthAndSixMonths() {
        // Arrange
        when(populationEventRepository.findPlantingsByOptionalFilters(null, null)).thenReturn(List.of());
        // Act
        YieldForecastResponse forecast = yieldForecastService.getYieldForecast(null, null, null, null, TODAY);
        // Assert: September 2026 to February 2027, all at zero
        assertEquals(YearMonth.of(2026, 9), forecast.from());
        assertEquals(6, forecast.months());
        assertEquals(YieldForecastCalculator.window(YearMonth.of(2026, 9), 6),
                forecast.monthlyTotals().stream().map(MonthlyTotal::month).toList());
        assertTrue(forecast.monthlyTotals().stream().allMatch(total -> total.expectedKg() == 0.0));
        assertTrue(forecast.entries().isEmpty());
        assertTrue(forecast.varietiesWithoutReference().isEmpty());
        verify(populationEventRepository, never()).findTreeBalances(anyCollection());
    }

    @Test
    void getYieldForecast_shouldPassTheFiltersAndTurnABlankBlockIntoNoFilter() {
        // Arrange
        when(populationEventRepository.findPlantingsByOptionalFilters(1, "B")).thenReturn(List.of());
        when(populationEventRepository.findPlantingsByOptionalFilters(null, null)).thenReturn(List.of());
        // Act
        yieldForecastService.getYieldForecast(1, " B ", JANUARY_2027, 12, TODAY);
        yieldForecastService.getYieldForecast(null, "  ", JANUARY_2027, 12, TODAY);
        // Assert
        verify(populationEventRepository).findPlantingsByOptionalFilters(1, "B");
        verify(populationEventRepository).findPlantingsByOptionalFilters(null, null);
    }

    @Test
    void getYieldForecast_shouldIgnoreAVarietyWithNoTreeLeft() {
        // Arrange: every Kent tree died; Palmer has no reference but no tree either
        Variety kent = variety(2L, null, "A", "Kent");
        Variety palmer = variety(4L, null, "A", "Palmer");
        plantings(null, null, List.of(
                        planting(kent, LocalDate.of(2020, 6, 1), 40),
                        planting(palmer, LocalDate.of(2024, 1, 10), 10)),
                0, -1);
        when(varietyReferenceRepository.findAll()).thenReturn(defaultReferences());
        when(growthPhaseYieldShareRepository.findAll()).thenReturn(shares(0.0, 0.5, 1.0));
        // Act
        YieldForecastResponse forecast = yieldForecastService.getYieldForecast(null, null, JANUARY_2027, 12, TODAY);
        // Assert
        assertTrue(forecast.entries().isEmpty());
        assertTrue(forecast.varietiesWithoutReference().isEmpty());
    }

    @Test
    void getYieldForecast_shouldGiveNothing_forTheMonthOfAPlantingAfterItsFirstDay() {
        // Arrange: a non-zero establishment share, so only the missing age can explain a missing entry
        Variety kent = variety(2L, null, "A", "Kent");
        plantings(null, null, List.of(planting(kent, LocalDate.of(2027, 4, 15), 40)), 40);
        when(varietyReferenceRepository.findAll()).thenReturn(defaultReferences());
        when(growthPhaseYieldShareRepository.findAll()).thenReturn(shares(0.1, 0.5, 1.0));
        // Act
        YieldForecastResponse forecast = yieldForecastService.getYieldForecast(null, null, JANUARY_2027, 12, TODAY);
        // Assert: nothing in April, planted on the 15th; May counts, at age 0
        assertEquals(1, forecast.entries().size());
        Entry may = forecast.entries().get(0);
        assertEquals(YearMonth.of(2027, 5), may.month());
        assertEquals(0, may.ageYears());
        assertEquals("establishment", may.growthPhase());
        assertEquals(400.0, may.expectedKg());
    }

    @Test
    void getYieldForecast_shouldSpreadASeasonOverTheNewYear() {
        // Arrange: a variety harvested from November to February
        Variety winter = variety(5L, null, "C", "Winter");
        plantings(null, null, List.of(planting(winter, LocalDate.of(2015, 1, 1), 10)), 10);
        when(varietyReferenceRepository.findAll())
                .thenReturn(List.of(reference(9L, "Winter", 100.0, 11, 2, "test_source")));
        when(growthPhaseYieldShareRepository.findAll()).thenReturn(shares(0.0, 0.5, 1.0));
        // Act
        YieldForecastResponse forecast = yieldForecastService.getYieldForecast(
                null, null, YearMonth.of(2026, 10), 6, TODAY);
        // Assert: 10 × 100 / 4 in November, December, January and February
        assertEquals(List.of(0.0, 250.0, 250.0, 250.0, 250.0, 0.0),
                forecast.monthlyTotals().stream().map(MonthlyTotal::expectedKg).toList());
        assertEquals(0.25, forecast.entries().get(0).monthShare());
    }

    @Test
    void getYieldForecast_shouldLogAndThrow_whenAPhaseHasNoShare() {
        // Arrange: the gradual production row was deleted by hand
        Variety keitt = variety(1L, null, "B", "Keitt");
        plantings(null, null, List.of(planting(keitt, LocalDate.of(2023, 9, 24), 150)), 150);
        when(varietyReferenceRepository.findAll()).thenReturn(defaultReferences());
        when(growthPhaseYieldShareRepository.findAll()).thenReturn(List.of(
                new GrowthPhaseYieldShare(1L, "establishment", 0.0, "orchard_literature", null),
                new GrowthPhaseYieldShare(3L, "full production", 1.0, "by_definition", null)));
        Logger logger = (Logger) LoggerFactory.getLogger(YieldForecastService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            // Act
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> yieldForecastService.getYieldForecast(null, null, JANUARY_2027, 12, TODAY));
            // Assert: an error line naming the phase, since the core handler logs nothing
            assertEquals("No yield share for the growth phase gradual production", ex.getMessage());
            assertEquals(1, appender.list.size());
            ILoggingEvent event = appender.list.get(0);
            assertEquals(Level.ERROR, event.getLevel());
            assertTrue(event.getFormattedMessage().contains("\"gradual production\""), event.getFormattedMessage());
        } finally {
            logger.detachAppender(appender);
        }
    }
}
