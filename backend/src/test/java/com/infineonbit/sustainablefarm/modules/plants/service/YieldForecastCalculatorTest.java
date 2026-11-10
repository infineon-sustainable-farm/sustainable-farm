package com.infineonbit.sustainablefarm.modules.plants.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class YieldForecastCalculatorTest {

    private static final YearMonth SEPTEMBER_2026 = YearMonth.of(2026, 9);

    /** Phase of trees planted on the given date, on the first day of September 2026. */
    private static String phaseInSeptember2026(LocalDate plantingDate) {
        return GrowthPhaseCalculator.computePhase(YieldForecastCalculator.ageAtStartOf(SEPTEMBER_2026, plantingDate));
    }

    @Test
    void window_shouldListTheMonthsInOrder_acrossTheNewYear() {
        // Act & Assert
        assertEquals(
                List.of(YearMonth.of(2026, 11), YearMonth.of(2026, 12), YearMonth.of(2027, 1), YearMonth.of(2027, 2)),
                YieldForecastCalculator.window(YearMonth.of(2026, 11), 4));
        assertEquals(List.of(SEPTEMBER_2026), YieldForecastCalculator.window(SEPTEMBER_2026, 1));
    }

    @Test
    void ageAtStartOf_shouldGiveThePhaseBoundaries_onTheFirstDayOfTheMonth() {
        // Act & Assert: 2 years 11 months, 3 years exactly, 5 years, 6 years exactly
        assertEquals("establishment", phaseInSeptember2026(LocalDate.of(2023, 10, 1)));
        assertEquals("gradual production", phaseInSeptember2026(LocalDate.of(2023, 9, 1)));
        assertEquals("gradual production", phaseInSeptember2026(LocalDate.of(2021, 9, 1)));
        assertEquals("full production", phaseInSeptember2026(LocalDate.of(2020, 9, 1)));
    }

    @Test
    void ageAtStartOf_shouldUseTheFirstDayOfTheMonth_notToday() {
        // Act
        Period age = YieldForecastCalculator.ageAtStartOf(YearMonth.of(2027, 5), LocalDate.of(2023, 9, 24));
        // Assert: 3 years 7 months on 2027-05-01
        assertEquals(3, age.getYears());
        assertEquals(7, age.getMonths());
    }

    @Test
    void ageAtStartOf_shouldReturnNull_whenPlantedAfterTheFirstDayOfTheMonth() {
        // Act & Assert: no tree on the first day of the month yet
        assertNull(YieldForecastCalculator.ageAtStartOf(SEPTEMBER_2026, LocalDate.of(2026, 9, 2)));
        assertEquals(Period.ZERO, YieldForecastCalculator.ageAtStartOf(SEPTEMBER_2026, LocalDate.of(2026, 9, 1)));
    }

    @Test
    void isInSeason_shouldIncludeBothEnds_ofASimpleSeason() {
        // Act & Assert: May to July
        assertFalse(YieldForecastCalculator.isInSeason(4, 5, 7));
        assertTrue(YieldForecastCalculator.isInSeason(5, 5, 7));
        assertTrue(YieldForecastCalculator.isInSeason(6, 5, 7));
        assertTrue(YieldForecastCalculator.isInSeason(7, 5, 7));
        assertFalse(YieldForecastCalculator.isInSeason(8, 5, 7));
    }

    @Test
    void isInSeason_shouldRunOverTheNewYear_whenTheEndIsBeforeTheStart() {
        // Act & Assert: November to February
        assertFalse(YieldForecastCalculator.isInSeason(10, 11, 2));
        assertTrue(YieldForecastCalculator.isInSeason(11, 11, 2));
        assertTrue(YieldForecastCalculator.isInSeason(12, 11, 2));
        assertTrue(YieldForecastCalculator.isInSeason(1, 11, 2));
        assertTrue(YieldForecastCalculator.isInSeason(2, 11, 2));
        assertFalse(YieldForecastCalculator.isInSeason(3, 11, 2));
    }

    @Test
    void seasonLength_shouldCountBothEnds() {
        // Act & Assert
        assertEquals(3, YieldForecastCalculator.seasonLength(5, 7));
        assertEquals(2, YieldForecastCalculator.seasonLength(4, 5));
        assertEquals(1, YieldForecastCalculator.seasonLength(6, 6));
        assertEquals(4, YieldForecastCalculator.seasonLength(11, 2));
        assertEquals(2, YieldForecastCalculator.seasonLength(12, 1));
    }

    @Test
    void monthShare_shouldSpreadTheYieldEvenlyOverTheSeason() {
        // Act & Assert: Keitt, May to July
        assertEquals(1.0 / 3, YieldForecastCalculator.monthShare(YearMonth.of(2027, 5), 5, 7));
        assertEquals(1.0 / 3, YieldForecastCalculator.monthShare(YearMonth.of(2027, 7), 5, 7));
        assertEquals(0.0, YieldForecastCalculator.monthShare(YearMonth.of(2027, 8), 5, 7));
        // Act & Assert: a season from November to February
        assertEquals(0.25, YieldForecastCalculator.monthShare(YearMonth.of(2026, 12), 11, 2));
        assertEquals(0.25, YieldForecastCalculator.monthShare(YearMonth.of(2027, 1), 11, 2));
        assertEquals(0.0, YieldForecastCalculator.monthShare(YearMonth.of(2027, 3), 11, 2));
    }

    /** Decimals with one digit after the point, as the spread returns them. */
    private static List<BigDecimal> kg(String... values) {
        return Arrays.stream(values).map(BigDecimal::new).toList();
    }

    @Test
    void yearlyKg_shouldMultiplyTheFactors_exactly() {
        // Act & Assert: Keitt, 40 × 220 × 0.25, with no binary noise
        assertEquals(0, new BigDecimal("2200").compareTo(YieldForecastCalculator.yearlyKg(40, 220.0, 0.25)));
        assertEquals(0, new BigDecimal("8800").compareTo(YieldForecastCalculator.yearlyKg(40, 220.0, 1.0)));
        // Establishment
        assertEquals(0, BigDecimal.ZERO.compareTo(YieldForecastCalculator.yearlyKg(20, 160.0, 0.0)));
    }

    @Test
    void seasonOf_shouldListTheWholeSeason_whateverTheMonth() {
        // Act & Assert: May to July 2027, from any of its months
        List<YearMonth> mayToJuly = List.of(YearMonth.of(2027, 5), YearMonth.of(2027, 6), YearMonth.of(2027, 7));
        assertEquals(mayToJuly, YieldForecastCalculator.seasonOf(YearMonth.of(2027, 5), 5, 7));
        assertEquals(mayToJuly, YieldForecastCalculator.seasonOf(YearMonth.of(2027, 7), 5, 7));
        // November to February: January 2027 is in the season that starts in November 2026
        List<YearMonth> novemberToFebruary = List.of(YearMonth.of(2026, 11), YearMonth.of(2026, 12),
                YearMonth.of(2027, 1), YearMonth.of(2027, 2));
        assertEquals(novemberToFebruary, YieldForecastCalculator.seasonOf(YearMonth.of(2027, 1), 11, 2));
        assertEquals(novemberToFebruary, YieldForecastCalculator.seasonOf(YearMonth.of(2026, 12), 11, 2));
        // A month out of the season has none
        assertThrows(IllegalArgumentException.class,
                () -> YieldForecastCalculator.seasonOf(YearMonth.of(2027, 3), 11, 2));
    }

    @Test
    void spreadOverSeason_shouldGiveTheMissingTenthToTheFirstMonth_onEqualRemainders() {
        // Act: 8,800 kg over three months, 2,933.333… each
        List<BigDecimal> months = YieldForecastCalculator.spreadOverSeason(kg("8800", "8800", "8800"));
        // Assert: the total is 8,800.0, not 3 × 2,933.3 = 8,799.9
        assertEquals(kg("2933.4", "2933.3", "2933.3"), months);
        assertEquals(new BigDecimal("8800.0"), months.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        // And 40 × 220 × 0.25 = 2,200 kg
        assertEquals(kg("733.4", "733.3", "733.3"), YieldForecastCalculator.spreadOverSeason(kg("2200", "2200", "2200")));
    }

    @Test
    void spreadOverSeason_shouldGiveTheMissingTenthsToTheLargestRemainders() {
        // Act: a phase change in the season; exact months 733.333…, 2,933.333…, 2,933.333…
        // Total 6,600.0; truncated 733.3 + 2,933.3 + 2,933.3 = 6,599.9; equal remainders: the earliest gets it
        assertEquals(kg("733.4", "2933.3", "2933.3"),
                YieldForecastCalculator.spreadOverSeason(kg("2200", "8800", "8800")));
        // Exact months 0.2666…, 0.2666…, 0.1666…: total 0.7; truncated 0.2 + 0.2 + 0.1 = 0.5;
        // the three remainders are 0.0666…: the first two months get a tenth each
        assertEquals(kg("0.3", "0.3", "0.1"), YieldForecastCalculator.spreadOverSeason(kg("0.8", "0.8", "0.5")));
        // Exact months 0.06, 0.09, 0.05: total 0.2; truncated 0; the largest remainders, 2nd then 1st, get a tenth
        assertEquals(kg("0.1", "0.1", "0.0"), YieldForecastCalculator.spreadOverSeason(kg("0.18", "0.27", "0.15")));
    }

    @Test
    void spreadOverSeason_shouldKeepARoundSeason_andAnEmptyOne() {
        // Act & Assert: 150 × 220 × 0.5 / 3 = 5,500 each, nothing to add
        assertEquals(kg("5500.0", "5500.0", "5500.0"),
                YieldForecastCalculator.spreadOverSeason(kg("16500", "16500", "16500")));
        // Trees planted during the season, then in establishment: nothing at all
        assertEquals(kg("0.0", "0.0", "0.0", "0.0"), YieldForecastCalculator.spreadOverSeason(kg("0", "0", "0", "0")));
        // Trees planted after the first month: that month gives nothing, the others share the season
        assertEquals(kg("0.0", "2933.4", "2933.3"), YieldForecastCalculator.spreadOverSeason(kg("0", "8800", "8800")));
    }

    @Test
    void roundToTenth_shouldRoundHalvesUp() {
        // Act & Assert
        assertEquals(12.3, YieldForecastCalculator.roundToTenth(12.34));
        assertEquals(12.4, YieldForecastCalculator.roundToTenth(12.35));
        assertEquals(0.1, YieldForecastCalculator.roundToTenth(0.05));
        assertEquals(0.0, YieldForecastCalculator.roundToTenth(0.04));
        assertEquals(0.3, YieldForecastCalculator.roundToTenth(0.1 + 0.2));
    }

    @Test
    void roundShare_shouldKeepFourDecimals() {
        // Act & Assert
        assertEquals(0.3333, YieldForecastCalculator.roundShare(1.0 / 3));
        assertEquals(0.6667, YieldForecastCalculator.roundShare(2.0 / 3));
        assertEquals(0.5, YieldForecastCalculator.roundShare(0.5));
    }
}
