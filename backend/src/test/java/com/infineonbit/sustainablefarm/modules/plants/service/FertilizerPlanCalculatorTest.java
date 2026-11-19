package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.money.CurrencyCode;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerMovement;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerMovementType;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerPlan;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerPlanStatus;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerProduct;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerType;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerUnit;
import com.infineonbit.sustainablefarm.modules.plants.service.FertilizerPlanCalculator.Amounts;
import com.infineonbit.sustainablefarm.modules.plants.service.FertilizerPlanCalculator.ComparisonLine;
import com.infineonbit.sustainablefarm.modules.plants.service.FertilizerPlanCalculator.LastPrice;
import com.infineonbit.sustainablefarm.modules.plants.service.FertilizerPlanCalculator.StockNeed;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FertilizerPlanCalculatorTest {

    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 14);
    private static final BigDecimal PARITY = new BigDecimal("655.957");

    private static final FertilizerProduct NPK = new FertilizerProduct(1L, "NPK 15-15-15", FertilizerType.MINERAL,
            "15-15-15", FertilizerUnit.KG, null, "user_entry", NOW);
    private static final FertilizerProduct LIQUID = new FertilizerProduct(2L, "Liquid feed", FertilizerType.ORGANIC,
            null, FertilizerUnit.L, null, "user_entry", NOW);

    private static FertilizerMovement purchase(long id, FertilizerProduct product, LocalDate date, String quantity,
                                               String cost, String currency) {
        FertilizerMovement purchase = new FertilizerMovement();
        purchase.setId(id);
        purchase.setProduct(product);
        purchase.setMovementType(FertilizerMovementType.PURCHASE);
        purchase.setMovementDate(date);
        purchase.setQuantity(new BigDecimal(quantity));
        purchase.setTotalCost(new BigDecimal(cost));
        purchase.setCurrency(currency);
        return purchase;
    }

    private static FertilizerMovement application(long id, FertilizerProduct product, String block, String quantity) {
        FertilizerMovement application = new FertilizerMovement();
        application.setId(id);
        application.setProduct(product);
        application.setMovementType(FertilizerMovementType.APPLICATION);
        application.setMovementDate(TODAY);
        application.setQuantity(new BigDecimal(quantity));
        application.setBlockCode(block);
        return application;
    }

    private static FertilizerPlan plan(long id, FertilizerProduct product, String block, LocalDate date,
                                       String quantity, FertilizerPlanStatus status) {
        FertilizerPlan plan = new FertilizerPlan();
        plan.setId(id);
        plan.setProduct(product);
        plan.setBlockCode(block);
        plan.setPlannedDate(date);
        plan.setPlannedQuantity(new BigDecimal(quantity));
        plan.setStatus(status);
        plan.setSource("user_entry");
        plan.setLastUpdated(NOW);
        return plan;
    }

    @Test
    void lastPrices_shouldKeepTheFirstPurchaseOfEachFertilizer() {
        // Arrange: the most recent first for each fertilizer, as the repository orders them
        List<FertilizerMovement> purchases = List.of(
                purchase(12L, NPK, LocalDate.of(2026, 11, 3), "200.000", "100000.00", "XOF"),
                purchase(10L, NPK, LocalDate.of(2026, 6, 1), "100.000", "40000.00", "XOF"),
                purchase(11L, LIQUID, LocalDate.of(2026, 9, 1), "20.000", "30.00", "EUR"));
        // Act
        Map<Long, LastPrice> prices = FertilizerPlanCalculator.lastPrices(purchases);
        // Assert
        assertEquals(2, prices.size());
        assertEquals(new LastPrice(new BigDecimal("100000.00"), new BigDecimal("200.000"), CurrencyCode.XOF,
                LocalDate.of(2026, 11, 3)), prices.get(1L));
        assertEquals(CurrencyCode.EUR, prices.get(2L).currency());
    }

    @Test
    void costOf_shouldGiveBothCurrencies_whenThePurchaseIsInFcfa() {
        // Arrange: 100,000 FCFA for 200 kg, so 500 FCFA per kg
        LastPrice price = new LastPrice(new BigDecimal("100000.00"), new BigDecimal("200.000"), CurrencyCode.XOF,
                TODAY);
        // Act
        Amounts cost = FertilizerPlanCalculator.costOf(new BigDecimal("30.000"), price, PARITY);
        // Assert: 15,000 FCFA / 655.957 = 22.867... euros
        assertEquals(15000L, cost.xof());
        assertEquals(22.87, cost.eur());
    }

    @Test
    void costOf_shouldConvertFromEuros_whenThePurchaseIsInEuros() {
        // Arrange: 30 euros for 20 L, so 1.50 euro per litre
        LastPrice price = new LastPrice(new BigDecimal("30.00"), new BigDecimal("20.000"), CurrencyCode.EUR, TODAY);
        // Act
        Amounts cost = FertilizerPlanCalculator.costOf(new BigDecimal("4.000"), price, PARITY);
        // Assert: 6 euros x 655.957 = 3,935.742 FCFA
        assertEquals(3936L, cost.xof());
        assertEquals(6.0, cost.eur());
    }

    @Test
    void costOf_shouldUseTheExactPrice_whenThePriceDoesNotDivideEvenly() {
        // Arrange: 1,000 FCFA for 3 kg, so 333.33... FCFA per kg
        LastPrice price = new LastPrice(new BigDecimal("1000.00"), new BigDecimal("3.000"), CurrencyCode.XOF, TODAY);
        // Act
        Amounts unit = FertilizerPlanCalculator.costOf(BigDecimal.ONE, price, null);
        Amounts cost = FertilizerPlanCalculator.costOf(new BigDecimal("3.000"), price, null);
        // Assert: three times the rounded unit price would give 999
        assertEquals(333L, unit.xof());
        assertEquals(1000L, cost.xof());
    }

    @Test
    void costOf_shouldGiveOnlyTheCurrencyOfThePurchase_whenNoRateIsRecorded() {
        // Arrange
        LastPrice inFcfa = new LastPrice(new BigDecimal("100000.00"), new BigDecimal("200.000"), CurrencyCode.XOF,
                TODAY);
        LastPrice inEuros = new LastPrice(new BigDecimal("30.00"), new BigDecimal("20.000"), CurrencyCode.EUR, TODAY);
        // Act
        Amounts fcfa = FertilizerPlanCalculator.costOf(new BigDecimal("30"), inFcfa, null);
        Amounts euros = FertilizerPlanCalculator.costOf(new BigDecimal("4"), inEuros, null);
        // Assert
        assertEquals(new Amounts(15000L, null), fcfa);
        assertEquals(new Amounts(null, 6.0), euros);
    }

    @Test
    void costOf_shouldGiveNoAmount_whenNoPurchaseHasACost() {
        // Act
        Amounts cost = FertilizerPlanCalculator.costOf(new BigDecimal("30"), null, PARITY);
        // Assert
        assertSame(Amounts.NONE, cost);
        assertNull(cost.xof());
        assertNull(cost.eur());
    }

    @Test
    void daysFromToday_shouldBePositiveBeforeTheDateAndNegativeAfterIt() {
        // Act + Assert
        assertEquals(10, FertilizerPlanCalculator.daysFromToday(TODAY.plusDays(10), TODAY));
        assertEquals(0, FertilizerPlanCalculator.daysFromToday(TODAY, TODAY));
        assertEquals(-3, FertilizerPlanCalculator.daysFromToday(TODAY.minusDays(3), TODAY));
    }

    @Test
    void overdue_shouldBeTrueOnlyForAPlannedApplicationWhoseDateHasPassed() {
        // Act + Assert: today is not late, and a final state never is
        assertTrue(FertilizerPlanCalculator.overdue(FertilizerPlanStatus.PLANNED, TODAY.minusDays(1), TODAY));
        assertFalse(FertilizerPlanCalculator.overdue(FertilizerPlanStatus.PLANNED, TODAY, TODAY));
        assertFalse(FertilizerPlanCalculator.overdue(FertilizerPlanStatus.PLANNED, TODAY.plusDays(1), TODAY));
        assertFalse(FertilizerPlanCalculator.overdue(FertilizerPlanStatus.DONE, TODAY.minusDays(1), TODAY));
        assertFalse(FertilizerPlanCalculator.overdue(FertilizerPlanStatus.CANCELLED, TODAY.minusDays(1), TODAY));
    }

    @Test
    void gapPercent_shouldCompareWithThePlannedQuantity() {
        // Act + Assert
        assertEquals(new BigDecimal("-25.0"),
                FertilizerPlanCalculator.gapPercent(new BigDecimal("40"), new BigDecimal("30")));
        assertEquals(new BigDecimal("33.3"),
                FertilizerPlanCalculator.gapPercent(new BigDecimal("30"), new BigDecimal("40")));
        assertEquals(new BigDecimal("0.0"),
                FertilizerPlanCalculator.gapPercent(new BigDecimal("30"), new BigDecimal("30.000")));
    }

    @Test
    void gapPercent_shouldBeNull_whenNothingWasPlanned() {
        // Act + Assert
        assertNull(FertilizerPlanCalculator.gapPercent(new BigDecimal("0.000"), new BigDecimal("30")));
    }

    @Test
    void compare_shouldTotalPlannedAndAppliedByBlockAndFertilizer() {
        // Arrange: on block A, 40 kg done with 35 kg, 20 kg still planned, 10 kg cancelled, and
        // 5 kg applied without a plan; on block B, litres applied without any plan
        FertilizerMovement applied = application(101L, NPK, "A", "35.000");
        FertilizerPlan done = plan(1L, NPK, "A", TODAY.minusDays(20), "40.000", FertilizerPlanStatus.DONE);
        done.setMovement(applied);
        List<FertilizerPlan> plans = List.of(
                done,
                plan(2L, NPK, "A", TODAY.plusDays(5), "20.000", FertilizerPlanStatus.PLANNED),
                plan(3L, NPK, "A", TODAY.plusDays(6), "10.000", FertilizerPlanStatus.CANCELLED));
        List<FertilizerMovement> applications = List.of(
                applied,
                application(102L, NPK, "A", "5.000"),
                application(103L, LIQUID, "B", "2.500"));
        // Act
        List<ComparisonLine> lines = FertilizerPlanCalculator.compare(plans, applications, Set.of(101L));
        // Assert: the application of the done plan is counted once, with its plan
        assertEquals(2, lines.size());
        ComparisonLine blockA = lines.get(0);
        assertEquals("A", blockA.blockCode());
        assertSame(NPK, blockA.product());
        assertEquals(new BigDecimal("60.000"), blockA.planned());
        assertEquals(new BigDecimal("20.000"), blockA.remaining());
        assertEquals(new BigDecimal("40.000"), blockA.applied());
        assertEquals(new BigDecimal("5.000"), blockA.unplanned());
        assertEquals(new BigDecimal("-20.000"), blockA.gap());
        ComparisonLine blockB = lines.get(1);
        assertEquals("B", blockB.blockCode());
        assertSame(LIQUID, blockB.product());
        assertEquals(BigDecimal.ZERO, blockB.planned());
        assertEquals(new BigDecimal("2.500"), blockB.applied());
        assertEquals(new BigDecimal("2.500"), blockB.unplanned());
    }

    @Test
    void compare_shouldCountTheApplicationOfADonePlanWithItsPlan_whateverItsOwnDate() {
        // Arrange: the plan is in the period, its application was made before the period started
        FertilizerPlan done = plan(1L, NPK, "A", TODAY.plusDays(10), "30.000", FertilizerPlanStatus.DONE);
        done.setMovement(application(101L, NPK, "A", "25.500"));
        // Act
        List<ComparisonLine> lines = FertilizerPlanCalculator.compare(List.of(done), List.of(), Set.of(101L));
        // Assert: the plan is not left at -100%
        assertEquals(1, lines.size());
        assertEquals(new BigDecimal("30.000"), lines.get(0).planned());
        assertEquals(new BigDecimal("25.500"), lines.get(0).applied());
        assertEquals(BigDecimal.ZERO, lines.get(0).unplanned());
        assertEquals(BigDecimal.ZERO, lines.get(0).remaining());
    }

    @Test
    void compare_shouldLeaveOutAnApplicationLinkedToAPlanOfAnotherPeriod() {
        // Arrange: the application is in the period, the plan it completes is not
        List<FertilizerMovement> applications = List.of(application(101L, NPK, "A", "25.500"));
        // Act
        List<ComparisonLine> lines = FertilizerPlanCalculator.compare(List.of(), applications, Set.of(101L));
        // Assert: it is counted with its plan, in the period of that plan
        assertTrue(lines.isEmpty());
    }

    @Test
    void compare_shouldOrderByBlockThenFertilizerName() {
        // Arrange
        List<FertilizerPlan> plans = List.of(
                plan(1L, NPK, "B", TODAY, "1", FertilizerPlanStatus.PLANNED),
                plan(2L, NPK, "A", TODAY, "1", FertilizerPlanStatus.PLANNED),
                plan(3L, LIQUID, "A", TODAY, "1", FertilizerPlanStatus.PLANNED));
        // Act
        List<ComparisonLine> lines = FertilizerPlanCalculator.compare(plans, List.of(), Set.of());
        // Assert
        assertEquals(List.of("A/Liquid feed", "A/NPK 15-15-15", "B/NPK 15-15-15"),
                lines.stream().map(line -> line.blockCode() + "/" + line.product().getName()).toList());
    }

    @Test
    void compare_shouldGiveNoLine_whenOnlyCancelledPlansExist() {
        // Act
        List<ComparisonLine> lines = FertilizerPlanCalculator.compare(
                List.of(plan(1L, NPK, "A", TODAY, "10", FertilizerPlanStatus.CANCELLED)), List.of(), Set.of());
        // Assert
        assertTrue(lines.isEmpty());
    }

    @Test
    void shortages_shouldListTheFertilizersPlannedBeyondTheirStock() {
        // Arrange: 70 kg of NPK planned, 30 of them late, for 50 kg in stock; 5 L planned for 5 L
        List<FertilizerPlan> openPlans = List.of(
                plan(1L, NPK, "A", TODAY.minusDays(2), "30.000", FertilizerPlanStatus.PLANNED),
                plan(2L, NPK, "B", TODAY.plusDays(10), "40.000", FertilizerPlanStatus.PLANNED),
                plan(3L, LIQUID, "A", TODAY.plusDays(3), "5.000", FertilizerPlanStatus.PLANNED));
        Map<Long, BigDecimal> stocks = Map.of(1L, new BigDecimal("50.000"), 2L, new BigDecimal("5.000"));
        // Act
        List<StockNeed> needs = FertilizerPlanCalculator.shortages(openPlans, stocks, TODAY);
        // Assert: a stock equal to the planned quantity covers it
        assertEquals(1, needs.size());
        StockNeed need = needs.get(0);
        assertSame(NPK, need.product());
        assertEquals(new BigDecimal("70.000"), need.planned());
        assertEquals(new BigDecimal("30.000"), need.overdue());
        assertEquals(new BigDecimal("50.000"), need.stock());
        assertEquals(new BigDecimal("20.000"), need.shortfall());
    }

    @Test
    void shortages_shouldCountAStockOfZero_whenTheFertilizerHasNoMovement() {
        // Arrange
        List<FertilizerPlan> openPlans = List.of(
                plan(1L, LIQUID, "A", TODAY.plusDays(3), "5.000", FertilizerPlanStatus.PLANNED));
        // Act
        List<StockNeed> needs = FertilizerPlanCalculator.shortages(openPlans, Map.of(), TODAY);
        // Assert
        assertEquals(1, needs.size());
        assertEquals(BigDecimal.ZERO, needs.get(0).stock());
        assertEquals(BigDecimal.ZERO, needs.get(0).overdue());
        assertEquals(new BigDecimal("5.000"), needs.get(0).shortfall());
    }
}
