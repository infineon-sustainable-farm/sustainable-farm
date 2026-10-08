package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.service.GrowthPhaseCalculator.GrowthPhase;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class GrowthPhaseCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 16);

    @Test
    void computeAge_shouldReturnNull_whenPlantingDateIsNull() {
        // Act
        Period age = GrowthPhaseCalculator.computeAge(null, TODAY);
        // Assert
        assertNull(age);
    }

    @Test
    void computeAge_shouldReturnNull_whenPlantingDateIsInTheFuture() {
        // Act
        Period age = GrowthPhaseCalculator.computeAge(TODAY.plusDays(1), TODAY);
        // Assert
        assertNull(age);
    }

    @Test
    void computeAge_shouldReturnCompletedYearsAndMonths_whenPlantingDateIsKnown() {
        // Act
        Period age = GrowthPhaseCalculator.computeAge(LocalDate.of(2024, 3, 17), TODAY);
        // Assert: 2 years, 5 months and 30 days — months are not rounded up
        assertEquals(2, age.getYears());
        assertEquals(5, age.getMonths());
    }

    @Test
    void computePhase_shouldReturnNull_whenAgeIsNull() {
        // Act & Assert
        assertNull(GrowthPhaseCalculator.computePhase(null));
    }

    @Test
    void computePhase_shouldReturnEstablishment_whenYearsAreZeroToTwo() {
        // Act & Assert
        assertEquals("establishment", GrowthPhaseCalculator.computePhase(Period.of(0, 0, 0)));
        assertEquals("establishment", GrowthPhaseCalculator.computePhase(Period.of(2, 11, 30)));
    }

    @Test
    void computePhase_shouldReturnGradualProduction_whenYearsAreThreeToFive() {
        // Act & Assert
        assertEquals("gradual production", GrowthPhaseCalculator.computePhase(Period.of(3, 0, 0)));
        // Year 5 is in both "3 to 5" and "5 to 7" in the source; resolved to gradual production.
        assertEquals("gradual production", GrowthPhaseCalculator.computePhase(Period.of(5, 11, 30)));
    }

    @Test
    void computePhase_shouldReturnFullProduction_whenYearsAreSixOrMore() {
        // Act & Assert
        assertEquals("full production", GrowthPhaseCalculator.computePhase(Period.of(6, 0, 0)));
        assertEquals("full production", GrowthPhaseCalculator.computePhase(Period.of(12, 0, 0)));
    }

    @Test
    void computePhaseYearsBand_shouldReturnNull_whenAgeIsNull() {
        // Act & Assert
        assertNull(GrowthPhaseCalculator.computePhaseYearsBand(null));
    }

    @Test
    void computePhaseYearsBand_shouldMatchPhase_atEveryPhaseBoundary() {
        // Same boundaries as the computePhase tests; each band must agree with its phase.
        // Act & Assert
        assertEquals("0–2 yrs", GrowthPhaseCalculator.computePhaseYearsBand(Period.of(0, 0, 0)));
        assertEquals("0–2 yrs", GrowthPhaseCalculator.computePhaseYearsBand(Period.of(2, 11, 30)));
        assertEquals("3–5 yrs", GrowthPhaseCalculator.computePhaseYearsBand(Period.of(3, 0, 0)));
        assertEquals("3–5 yrs", GrowthPhaseCalculator.computePhaseYearsBand(Period.of(5, 11, 30)));
        assertEquals("6+ yrs", GrowthPhaseCalculator.computePhaseYearsBand(Period.of(6, 0, 0)));
        assertEquals("6+ yrs", GrowthPhaseCalculator.computePhaseYearsBand(Period.of(12, 0, 0)));
    }

    @Test
    void phases_shouldListEveryPhase_youngestFirst_withItsCodeAndBand() {
        // Act & Assert: the same labels and bands as computePhase and computePhaseYearsBand
        assertEquals(List.of(
                        new GrowthPhase("ESTABLISHMENT", "establishment", "0–2 yrs"),
                        new GrowthPhase("GRADUAL_PRODUCTION", "gradual production", "3–5 yrs"),
                        new GrowthPhase("FULL_PRODUCTION", "full production", "6+ yrs")),
                GrowthPhaseCalculator.phases());
    }
}
