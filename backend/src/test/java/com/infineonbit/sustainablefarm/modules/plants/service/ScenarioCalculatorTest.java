package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.entity.CurrencyCode;
import com.infineonbit.sustainablefarm.modules.plants.service.ScenarioCalculator.Change;
import com.infineonbit.sustainablefarm.modules.plants.service.ScenarioCalculator.Line;
import com.infineonbit.sustainablefarm.modules.plants.service.ScenarioCalculator.Plan;
import com.infineonbit.sustainablefarm.modules.plants.service.ScenarioCalculator.PlanYear;
import com.infineonbit.sustainablefarm.modules.plants.service.ScenarioCalculator.Variant;
import com.infineonbit.sustainablefarm.modules.plants.service.ScenarioCalculator.YearPhase;
import com.infineonbit.sustainablefarm.modules.plants.service.ScenarioCalculator.YearResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The expected values are computed by hand, in decimals, from the inputs of
 * each test: none is read from the code under test.
 */
public class ScenarioCalculatorTest {

    /** The dev scenario: 200 trees of 220 kg, 0.9072 €/kg, 6,707 € a year, 171,512 € invested. */
    private static Plan zalka() {
        return new Plan(200, new BigDecimal("220.0"), null, new BigDecimal("0.9072"), CurrencyCode.EUR,
                new BigDecimal("6707.00"), null, new BigDecimal("171512.00"));
    }

    /** A plan with every optional value entered. */
    private static Plan complete() {
        return new Plan(100, new BigDecimal("200.0"), new BigDecimal("0.2"), new BigDecimal("0.5"),
                CurrencyCode.EUR, new BigDecimal("1000.00"), new BigDecimal("0.05"), new BigDecimal("50000.00"));
    }

    private static Plan withoutPrice(Plan plan) {
        return new Plan(plan.treeCount(), plan.yieldPerTreeKg(), plan.lossRate(), null, plan.currency(),
                plan.annualFixedCost(), plan.variableCostPerKg(), plan.investment());
    }

    /** The default shares of the forecast: none for 0–2 years, a quarter for 3–5, all from 6. */
    private static List<YearPhase> defaultPhases(int horizonYears) {
        List<YearPhase> phases = new ArrayList<>();
        for (int year = 1; year <= horizonYears; year++) {
            int age = year - 1;
            phases.add(age <= 2
                    ? new YearPhase(year, age, "establishment", BigDecimal.ZERO)
                    : age <= 5
                    ? new YearPhase(year, age, "gradual production", new BigDecimal("0.25"))
                    : new YearPhase(year, age, "full production", BigDecimal.ONE));
        }
        return phases;
    }

    private static BigDecimal d(String value) {
        return new BigDecimal(value);
    }

    /** Compares decimals by value, so 44000.0 equals 44000.00. */
    private static void assertDecimal(String expected, BigDecimal actual) {
        assertEquals(0, d(expected).compareTo(actual), () -> "expected " + expected + ", got " + actual);
    }

    private static List<String> describe(List<Line> lines) {
        return lines.stream()
                .map(line -> line.type() + " " + line.quantityKg().toPlainString() + " kg × "
                        + line.unitAmount().toPlainString() + " = " + line.amount().toPlainString())
                .toList();
    }

    @Test
    void fullProduction_shouldGiveTheHarvestRevenueCostAndNetBenefit() {
        // Act
        YearResult year = ScenarioCalculator.fullProduction(zalka());
        // Assert: 200 × 220 kg; 44,000 × 0.9072; the fixed cost alone, no variable cost entered
        assertDecimal("44000.0", year.harvestKg());
        assertNull(year.lossKg());
        assertDecimal("44000.0", year.sellableKg());
        assertEquals(d("39916.80"), year.revenue());
        assertEquals(d("6707.00"), year.operatingCost());
        assertEquals(d("33209.80"), year.netBenefit());
    }

    @Test
    void fullProduction_shouldDeductTheLosses_andAddTheVariableCostOfTheQuantitySold() {
        // Act
        YearResult year = ScenarioCalculator.fullProduction(complete());
        // Assert: 20,000 kg, 20 % lost; 16,000 × 0.5; 1,000 + 0.05 × 16,000
        assertDecimal("20000.0", year.harvestKg());
        assertDecimal("4000.0", year.lossKg());
        assertDecimal("16000.0", year.sellableKg());
        assertEquals(d("8000.00"), year.revenue());
        assertEquals(d("1800.00"), year.operatingCost());
        assertEquals(d("6200.00"), year.netBenefit());
    }

    @Test
    void fullProduction_shouldRoundAmountsInFcfaToTheFranc() {
        // Arrange: 3 trees of 10.5 kg sold 333.3333 FCFA per kg
        Plan plan = new Plan(3, d("10.5"), null, d("333.3333"), CurrencyCode.XOF, d("1000"), d("2.5"), d("50000"));
        // Act
        YearResult year = ScenarioCalculator.fullProduction(plan);
        // Assert: 31.5 × 333.3333 = 10,499.99895; 1,000 + 2.5 × 31.5 = 1,078.75
        assertEquals(d("10500"), year.revenue());
        assertEquals(d("1079"), year.operatingCost());
        assertEquals(d("9421"), year.netBenefit());
    }

    @Test
    void fullProduction_shouldGiveNoRevenueNorNetBenefit_withoutAPrice() {
        // Act
        YearResult year = ScenarioCalculator.fullProduction(withoutPrice(zalka()));
        // Assert: the cost is still known, the rest is not, and never 0
        assertDecimal("44000.0", year.harvestKg());
        assertEquals(d("6707.00"), year.operatingCost());
        assertNull(year.revenue());
        assertNull(year.netBenefit());
    }

    @Test
    void roiAndPayback_shouldBeRoundedToOneDecimal() {
        // Act & Assert: 33,209.80 × 100 / 171,512 = 19.36…; 171,512 / 33,209.80 = 5.16…
        assertEquals(d("19.4"), ScenarioCalculator.roiPct(d("33209.80"), d("171512.00")));
        assertEquals(d("5.2"), ScenarioCalculator.paybackYears(d("171512.00"), d("33209.80")));
    }

    @Test
    void roiAndPayback_shouldBeUnknown_whenAValueIsUnknown_orTheBenefitIsNotPositive() {
        // Act & Assert
        assertNull(ScenarioCalculator.roiPct(null, d("171512.00")));
        assertNull(ScenarioCalculator.roiPct(d("100.00"), null));
        assertEquals(d("-2.0"), ScenarioCalculator.roiPct(d("-1000.00"), d("50000.00")));
        assertNull(ScenarioCalculator.paybackYears(d("50000.00"), d("-1000.00")));
        assertNull(ScenarioCalculator.paybackYears(d("50000.00"), d("0.00")));
        assertNull(ScenarioCalculator.paybackYears(d("50000.00"), null));
    }

    @Test
    void years_shouldFollowTheGrowthPhases_andFindThePaybackYear() {
        // Act
        List<PlanYear> years = ScenarioCalculator.years(zalka(), defaultPhases(12));
        // Assert: no harvest in years 1-3, a quarter in years 4-6, all from year 7; the fixed cost every year
        assertEquals(12, years.size());
        assertDecimal("0.0", years.get(0).result().harvestKg());
        assertEquals(d("-6707.00"), years.get(0).result().netBenefit());
        assertEquals(d("-178219.00"), years.get(0).cumulativeNetBenefit());
        assertEquals(d("-191633.00"), years.get(2).cumulativeNetBenefit());
        assertDecimal("11000.0", years.get(3).result().harvestKg());
        assertEquals(d("9979.20"), years.get(3).result().revenue());
        assertEquals(d("3272.20"), years.get(3).result().netBenefit());
        assertEquals(d("-181816.40"), years.get(5).cumulativeNetBenefit());
        assertDecimal("44000.0", years.get(6).result().harvestKg());
        assertEquals(d("-15767.40"), years.get(10).cumulativeNetBenefit());
        assertEquals(d("17442.40"), years.get(11).cumulativeNetBenefit());
        assertEquals(12, ScenarioCalculator.paybackYear(years));
    }

    @Test
    void paybackYear_shouldBeUnknown_whenNotReachedWithinTheHorizon_orWithoutAPrice() {
        // Act & Assert
        assertNull(ScenarioCalculator.paybackYear(ScenarioCalculator.years(zalka(), defaultPhases(10))));
        List<PlanYear> withoutPrice = ScenarioCalculator.years(withoutPrice(zalka()), defaultPhases(12));
        assertNull(withoutPrice.get(11).cumulativeNetBenefit());
        assertEquals(d("6707.00"), withoutPrice.get(11).result().operatingCost());
        assertNull(ScenarioCalculator.paybackYear(withoutPrice));
    }

    @Test
    void change_shouldGiveTheBenefitOfAHigherYieldPerTree() {
        // Arrange: 10 % more per tree, nothing else
        Variant variant = new Variant(0, d("10"), null, null);
        BigDecimal additional = ScenarioCalculator.additionalInvestment(variant, null, CurrencyCode.EUR);
        Plan variantPlan = ScenarioCalculator.apply(zalka(), variant, additional);
        // Act
        Change change = ScenarioCalculator.change(zalka(), variantPlan, additional);
        // Assert: 242 kg per tree, 48,400 kg; 48,400 × 0.9072 = 43,908.48
        assertDecimal("242.0", variantPlan.yieldPerTreeKg());
        assertEquals(List.of("YIELD_PER_TREE 4400.0 kg × 0.9072 = 3991.68"), describe(change.lines()));
        assertEquals(d("3991.68"), change.annualBenefit());
        assertEquals(d("3991.68"), change.revenue());
        assertDecimal("4400.0", change.harvestKg());
        assertEquals(d("0.00"), change.operatingCost());
        // 21.7 - 19.4, from the rounded returns
        assertEquals(d("2.3"), change.roiPctPoints());
        assertEquals(d("0.00"), change.additionalInvestment());
        assertNull(change.additionalInvestmentPaybackYears());
    }

    @Test
    void change_shouldSplitTheBenefitByCause_andTheLinesShouldAddUpToIt() {
        // Arrange: 10 trees more at 10 each plus 500, 5 % more per tree, losses from 20 % down to 10 %
        Variant variant = new Variant(10, d("5"), d("0.1"), d("500"));
        BigDecimal additional = ScenarioCalculator.additionalInvestment(variant, d("10.00"), CurrencyCode.EUR);
        Plan variantPlan = ScenarioCalculator.apply(complete(), variant, additional);
        // Act
        Change change = ScenarioCalculator.change(complete(), variantPlan, additional);
        // Assert: 16,000 kg sold, then 17,600 with the trees, 18,480 with the yield, 20,790 with the losses
        assertEquals(List.of(
                        "TREE_COUNT 1600.0 kg × 0.5 = 800.00",
                        "YIELD_PER_TREE 880.0 kg × 0.5 = 440.00",
                        "LOSS_RATE 2310.0 kg × 0.5 = 1155.00",
                        "VARIABLE_OPERATING_COST 4790.0 kg × 0.05 = -239.50"),
                describe(change.lines()));
        BigDecimal sum = change.lines().stream().map(Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(d("2155.50"), change.annualBenefit());
        assertEquals(0, sum.compareTo(change.annualBenefit()));
        assertEquals(10, change.treeCountChange());
        assertEquals(d("600.00"), change.additionalInvestment());
        // 8,355.50 × 100 / 50,600 = 16.5, against 12.4; 600 / 2,155.50 = 0.28 year
        assertEquals(d("4.1"), change.roiPctPoints());
        assertEquals(d("0.3"), change.additionalInvestmentPaybackYears());
    }

    @Test
    void change_shouldGiveNoBenefitLine_withoutAPrice() {
        // Arrange
        Plan plan = withoutPrice(zalka());
        Variant variant = new Variant(0, d("10"), null, null);
        Plan variantPlan = ScenarioCalculator.apply(plan, variant, d("0.00"));
        // Act
        Change change = ScenarioCalculator.change(plan, variantPlan, d("0.00"));
        // Assert: the quantities change, the money is unknown
        assertDecimal("4400.0", change.harvestKg());
        assertEquals(List.of(), change.lines());
        assertNull(change.annualBenefit());
        assertNull(change.revenue());
        assertNull(change.roiPctPoints());
    }

    @Test
    void additionalInvestment_shouldBeUnknown_whenTreesAreAddedWithoutACostPerTree() {
        // Arrange
        Variant variant = new Variant(20, null, null, null);
        // Act
        BigDecimal additional = ScenarioCalculator.additionalInvestment(variant, null, CurrencyCode.EUR);
        Plan variantPlan = ScenarioCalculator.apply(zalka(), variant, additional);
        Change change = ScenarioCalculator.change(zalka(), variantPlan, additional);
        // Assert: the benefit is known, the investment and the return are not
        assertNull(additional);
        assertNull(variantPlan.investment());
        assertEquals(d("3991.68"), change.annualBenefit());
        assertNull(change.roiPctPoints());
        assertNull(change.additionalInvestmentPaybackYears());
        assertNull(ScenarioCalculator.paybackYear(ScenarioCalculator.years(variantPlan, defaultPhases(12))));
    }

    @Test
    void additionalInvestment_shouldSaveNothing_whenTreesAreRemoved() {
        // Act & Assert: only the additional investment given counts
        assertEquals(d("0.00"), ScenarioCalculator.additionalInvestment(
                new Variant(-20, null, null, null), d("10.00"), CurrencyCode.EUR));
        assertEquals(d("750"), ScenarioCalculator.additionalInvestment(
                new Variant(-20, null, null, d("749.5")), null, CurrencyCode.XOF));
    }

    @Test
    void apply_shouldRoundTheYieldPerTreeOfTheVariantToOneDecimal() {
        // Act: 220 × 1.033 = 227.26
        Plan variantPlan = ScenarioCalculator.apply(zalka(), new Variant(0, d("3.3"), null, null), d("0.00"));
        // Assert
        assertDecimal("227.3", variantPlan.yieldPerTreeKg());
        assertDecimal("45460.0", ScenarioCalculator.fullProduction(variantPlan).harvestKg());
    }
}
