package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.ScenarioVariantParams;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.GrowthPhaseYieldShareResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.ScenarioAnalysisResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.ScenarioAnalysisResponse.Amount;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.ScenarioAnalysisResponse.BenefitLine;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.ScenarioAnalysisResponse.Projection;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.ScenarioAnalysisResponse.Year;
import com.infineonbit.sustainablefarm.modules.plants.entity.CurrencyRate;
import com.infineonbit.sustainablefarm.modules.plants.entity.PlantScenario;
import com.infineonbit.sustainablefarm.modules.plants.exception.PlantScenarioNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.CurrencyRateRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.PlantScenarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The amounts in FCFA are the amounts in euros × 655.957, rounded to the franc,
 * computed by hand.
 */
@ExtendWith(MockitoExtension.class)
public class ScenarioAnalysisServiceTest {

    private static final Instant ENTERED = Instant.parse("2026-10-25T08:00:00Z");
    private static final ScenarioVariantParams NO_VARIANT = new ScenarioVariantParams(null, null, null, null, null);

    @Mock
    private PlantScenarioRepository plantScenarioRepository;

    @Mock
    private GrowthPhaseYieldShareService growthPhaseYieldShareService;

    @Mock
    private CurrencyRateRepository currencyRateRepository;

    @InjectMocks
    private ScenarioAnalysisService scenarioAnalysisService;

    private static GrowthPhaseYieldShareResponse share(String code, String phase, String band, double value,
                                                       String source) {
        return new GrowthPhaseYieldShareResponse(code, phase, band, value, source, false, value, source, null);
    }

    private static List<GrowthPhaseYieldShareResponse> defaultShares(double gradualShare, String gradualSource) {
        return List.of(
                share("ESTABLISHMENT", "establishment", "0–2 yrs", 0.0, "orchard_literature"),
                share("GRADUAL_PRODUCTION", "gradual production", "3–5 yrs", gradualShare, gradualSource),
                share("FULL_PRODUCTION", "full production", "6+ yrs", 1.0, "by_definition"));
    }

    /** The dev scenario, in euros, without loss rate, variable cost nor cost per added tree. */
    private static PlantScenario zalka() {
        return new PlantScenario(1L, "Zalka 2025", "Keitt", 200, "Zalka_2025_C43",
                220.0, "derived: Zalka_2025 D4 / B4", null, null,
                new BigDecimal("0.9072"), "derived: Zalka_2025 R19 / D4", "EUR",
                new BigDecimal("6707.00"), "Zalka_2025_L23", null, null,
                new BigDecimal("171512.00"), "Zalka_2025_H28", null, null, ENTERED);
    }

    private void stored(PlantScenario scenario, double gradualShare, String gradualSource, String rate) {
        when(plantScenarioRepository.findById(scenario.getId())).thenReturn(Optional.of(scenario));
        when(growthPhaseYieldShareService.getAllShares()).thenReturn(defaultShares(gradualShare, gradualSource));
        when(currencyRateRepository.findByBaseCurrencyAndQuoteCurrency("EUR", "XOF")).thenReturn(rate == null
                ? Optional.empty()
                : Optional.of(new CurrencyRate(1L, "EUR", "XOF", new BigDecimal(rate), "BCEAO_fixed_parity_1999",
                ENTERED)));
    }

    private static String describe(Year year) {
        return year.year() + " " + year.ageYears() + " " + year.growthPhase() + " " + year.yieldShare() + " "
                + year.harvestKg() + " kg " + year.netBenefit() + " cumulative " + year.cumulativeNetBenefit();
    }

    @Test
    void getAnalysis_shouldGiveTheScenarioInBothCurrencies_overTenYears_withoutVariant() {
        // Arrange
        stored(zalka(), 0.25, "Bally_2002", "655.957");
        // Act
        ScenarioAnalysisResponse analysis = scenarioAnalysisService.getAnalysis(1L, NO_VARIANT);
        // Assert: the scenario, its layer and what is missing
        assertEquals(1L, analysis.scenarioId());
        assertEquals("Zalka 2025", analysis.name());
        assertEquals("scenario", analysis.basis());
        assertEquals("EUR", analysis.currency());
        assertEquals(655.957, analysis.eurToXofRate());
        assertEquals(10, analysis.horizonYears());
        assertEquals(List.of("lossRate", "variableCostPerKg", "costPerAdditionalTree"), analysis.missingInputs());
        assertEquals(defaultShares(0.25, "Bally_2002"), analysis.phaseShares());
        assertNull(analysis.variant());
        assertNull(analysis.change());
        // Assert: a year of full production, in euros and in FCFA
        Projection baseline = analysis.baseline();
        assertEquals(200, baseline.treeCount());
        assertEquals(220.0, baseline.yieldPerTreeKg());
        assertNull(baseline.lossRate());
        assertEquals(44000.0, baseline.harvestKg());
        assertNull(baseline.lossKg());
        assertEquals(44000.0, baseline.sellableKg());
        assertEquals(new Amount(26183704L, 39916.80), baseline.revenue());
        assertEquals(new Amount(4399504L, 6707.00), baseline.operatingCost());
        assertEquals(new Amount(21784201L, 33209.80), baseline.netBenefit());
        assertEquals(new Amount(112504497L, 171512.00), baseline.investment());
        assertEquals(19.4, baseline.roiPct());
        assertEquals(5.2, baseline.simplePaybackYears());
        // Assert: ten years, not enough to recover the investment with the growth phases
        assertEquals(10, baseline.years().size());
        assertEquals("1 0 establishment 0.0 0.0 kg Amount[xof=-4399504, eur=-6707.0] "
                + "cumulative Amount[xof=-116904001, eur=-178219.0]", describe(baseline.years().get(0)));
        assertEquals("4 3 gradual production 0.25 11000.0 kg Amount[xof=2146422, eur=3272.2] "
                + "cumulative Amount[xof=-123556585, eur=-188360.8]", describe(baseline.years().get(3)));
        assertEquals("10 9 full production 1.0 44000.0 kg Amount[xof=21784201, eur=33209.8] "
                + "cumulative Amount[xof=-32126937, eur=-48977.2]", describe(baseline.years().get(9)));
        assertNull(baseline.paybackYear());
    }

    @Test
    void getAnalysis_shouldFindThePaybackYear_whenTheHorizonIsLongEnough() {
        // Arrange
        stored(zalka(), 0.25, "Bally_2002", "655.957");
        // Act
        ScenarioAnalysisResponse analysis = scenarioAnalysisService.getAnalysis(1L,
                new ScenarioVariantParams(null, null, null, null, 12));
        // Assert
        assertEquals(12, analysis.horizonYears());
        assertEquals(12, analysis.baseline().years().size());
        assertEquals(new Amount(11441464L, 17442.40), analysis.baseline().years().get(11).cumulativeNetBenefit());
        assertEquals(12, analysis.baseline().paybackYear());
    }

    @Test
    void getAnalysis_shouldUseTheShareInEffect_whenTheUserCorrectedIt() {
        // Arrange: gradual production corrected to a half
        stored(zalka(), 0.5, "user_entry", "655.957");
        // Act
        ScenarioAnalysisResponse analysis = scenarioAnalysisService.getAnalysis(1L,
                new ScenarioVariantParams(null, null, null, null, 12));
        // Assert: 22,000 kg in year 4, and the investment recovered in year 11
        assertEquals(22000.0, analysis.baseline().years().get(3).harvestKg());
        assertEquals("user_entry", analysis.phaseShares().get(1).source());
        assertEquals(11, analysis.baseline().paybackYear());
    }

    @Test
    void getAnalysis_shouldGiveTheAmountsInTheScenarioCurrencyOnly_withoutARate() {
        // Arrange
        stored(zalka(), 0.25, "Bally_2002", null);
        // Act
        ScenarioAnalysisResponse analysis = scenarioAnalysisService.getAnalysis(1L, NO_VARIANT);
        // Assert
        assertNull(analysis.eurToXofRate());
        assertEquals(new Amount(null, 39916.80), analysis.baseline().revenue());
    }

    @Test
    void getAnalysis_shouldConvertAScenarioInFcfaToEuros() {
        // Arrange: 3 trees of 10.5 kg at 333.3333 FCFA, 1,000 FCFA a year plus 2.5 per kg
        PlantScenario scenario = new PlantScenario(2L, "Small plot", null, 3, "user_entry", 10.5, "user_entry",
                null, null, new BigDecimal("333.3333"), "market_survey", "XOF", new BigDecimal("1000.00"),
                "user_entry", new BigDecimal("2.5000"), "user_entry", new BigDecimal("50000.00"), "user_entry",
                null, null, ENTERED);
        stored(scenario, 0.25, "Bally_2002", "655.957");
        // Act
        ScenarioAnalysisResponse analysis = scenarioAnalysisService.getAnalysis(2L, NO_VARIANT);
        // Assert: rounded to the franc, then converted to the cent
        assertEquals("XOF", analysis.currency());
        assertEquals(List.of("lossRate", "costPerAdditionalTree"), analysis.missingInputs());
        assertEquals(new Amount(10500L, 16.01), analysis.baseline().revenue());
        assertEquals(new Amount(1079L, 1.64), analysis.baseline().operatingCost());
        assertEquals(new Amount(9421L, 14.36), analysis.baseline().netBenefit());
    }

    @Test
    void getAnalysis_shouldGiveTheVariantAndItsAnnualBenefit() {
        // Arrange
        stored(zalka(), 0.25, "Bally_2002", "655.957");
        // Act: 10 % more per tree
        ScenarioAnalysisResponse analysis = scenarioAnalysisService.getAnalysis(1L,
                new ScenarioVariantParams(null, 10.0, null, null, null));
        // Assert
        assertEquals(242.0, analysis.variant().yieldPerTreeKg());
        assertEquals(48400.0, analysis.variant().harvestKg());
        assertEquals(21.7, analysis.variant().roiPct());
        assertEquals(10, analysis.variant().years().size());
        assertEquals(0, analysis.change().treeCount());
        assertEquals(4400.0, analysis.change().harvestKg());
        assertEquals(2.3, analysis.change().roiPctPoints());
        assertEquals(List.of(new BenefitLine("YIELD_PER_TREE", 4400.0, 0.9072, new Amount(2618370L, 3991.68))),
                analysis.change().benefitLines());
        assertEquals(new Amount(2618370L, 3991.68), analysis.change().annualBenefit());
        assertEquals(new Amount(0L, 0.0), analysis.change().additionalInvestment());
        assertNull(analysis.change().additionalInvestmentPaybackYears());
    }

    @Test
    void getAnalysis_shouldAcceptAVariantThatKeepsOneTree() {
        // Arrange
        stored(zalka(), 0.25, "Bally_2002", "655.957");
        // Act
        ScenarioAnalysisResponse analysis = scenarioAnalysisService.getAnalysis(1L,
                new ScenarioVariantParams(-199, null, null, null, null));
        // Assert
        assertEquals(1, analysis.variant().treeCount());
        assertEquals(-199, analysis.change().treeCount());
    }

    @Test
    void getAnalysis_shouldRefuseAVariantThatLeavesNoTree() {
        // Arrange
        when(plantScenarioRepository.findById(1L)).thenReturn(Optional.of(zalka()));
        // Act
        BusinessRuleException refused = assertThrows(BusinessRuleException.class, () -> scenarioAnalysisService
                .getAnalysis(1L, new ScenarioVariantParams(-200, null, null, null, null)));
        // Assert
        assertEquals("The scenario has 200 trees, so treeCountChange must be -199 or more", refused.getMessage());
        verifyNoInteractions(growthPhaseYieldShareService, currencyRateRepository);
    }

    @Test
    void getAnalysis_shouldRefuseAnotherLossRate_whenTheScenarioHasNone() {
        // Arrange
        when(plantScenarioRepository.findById(1L)).thenReturn(Optional.of(zalka()));
        // Act
        BusinessRuleException refused = assertThrows(BusinessRuleException.class, () -> scenarioAnalysisService
                .getAnalysis(1L, new ScenarioVariantParams(null, null, 0.1, null, null)));
        // Assert
        assertEquals("The scenario has no loss rate: enter it before simulating another loss rate",
                refused.getMessage());
    }

    @Test
    void getAnalysis_shouldThrowNotFound_forAnUnknownId() {
        // Arrange
        when(plantScenarioRepository.findById(99L)).thenReturn(Optional.empty());
        // Act
        PlantScenarioNotFoundException refused = assertThrows(PlantScenarioNotFoundException.class,
                () -> scenarioAnalysisService.getAnalysis(99L, NO_VARIANT));
        // Assert
        assertEquals("Scenario with ID 99 not found", refused.getMessage());
    }
}
