package com.infineonbit.sustainablefarm.modules.plants.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    void expectedKg_shouldMultiplyTheFactors_withTheExactMonthShare() {
        // Act & Assert: Keitt, 150 × 220 × 0.5 / 3; with 0.3333 it would be 5499.5
        assertEquals(5500.0, YieldForecastCalculator.expectedKg(150, 220.0, 0.5, 1.0 / 3));
        // Kent, 40 × 200 × 1 / 2
        assertEquals(4000.0, YieldForecastCalculator.expectedKg(40, 200.0, 1.0, 0.5));
        // Establishment, and out of season
        assertEquals(0.0, YieldForecastCalculator.expectedKg(20, 160.0, 0.0, 1.0 / 3));
        assertEquals(0.0, YieldForecastCalculator.expectedKg(150, 220.0, 0.5, 0.0));
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
