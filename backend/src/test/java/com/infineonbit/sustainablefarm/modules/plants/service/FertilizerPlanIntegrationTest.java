package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.ApplicationRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.FertilizerPlanCompletionRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.FertilizerPlanRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.FertilizerRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PurchaseRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerMovementResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerPlanComparisonResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerPlanResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerPlanStockCheckResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerMovementType;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerPlanStatus;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The planned applications through the real services, their transactions and
 * the database: the queries with optional filters, the link between a plan
 * and its real application, the rollback of a refused one, and two requests
 * on the same plan sent together.
 *
 * <p>Each test works on its own fertilizer and its own block, so the rows
 * left by the others do not count. No EUR to XOF rate is entered: only the
 * amounts in FCFA, the currency of the purchases, are asserted.
 */
@SpringBootTest
@ActiveProfiles("test")
class FertilizerPlanIntegrationTest {

    /** Each race runs this many times, since H2 does not reproduce it on every trial. */
    private static final int TRIALS = 10;

    private static final LocalDate TODAY = LocalDate.now();

    @Autowired
    private FertilizerService fertilizerService;

    @Autowired
    private FertilizerMovementService fertilizerMovementService;

    @Autowired
    private FertilizerPlanService fertilizerPlanService;

    /** A new fertilizer with 50 units bought for 25,000 FCFA a month ago: 500 FCFA per unit. */
    private Long fertilizerWith50(String name, String unit) {
        Long id = fertilizerService.createFertilizer(new FertilizerRequest(name, "MINERAL", null, unit, null)).id();
        fertilizerMovementService.recordPurchase(id,
                new PurchaseRequest(TODAY.minusDays(30), 50.0, "Supplier A", 25000.0, "XOF"));
        return id;
    }

    private FertilizerPlanResponse plan(Long fertilizerId, String block, LocalDate date, double quantity) {
        return fertilizerPlanService.createPlan(
                new FertilizerPlanRequest(fertilizerId, null, block, date, quantity, null));
    }

    private static FertilizerPlanCompletionRequest completion(double quantity) {
        return new FertilizerPlanCompletionRequest(TODAY, quantity, "Team A", "around the tree base");
    }

    private FertilizerPlanResponse reread(Long fertilizerId, Long planId) {
        return fertilizerPlanService.getAllPlans(fertilizerId, null, null, null, null, null).stream()
                .filter(plan -> plan.id().equals(planId))
                .findFirst()
                .orElseThrow();
    }

    /**
     * Runs the calls together and returns, for each one in order, its result
     * or the exception it threw.
     */
    private static List<Object> together(List<Callable<Object>> calls) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(calls.size());
        CountDownLatch ready = new CountDownLatch(calls.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Object>> futures = new ArrayList<>();
        try {
            for (Callable<Object> call : calls) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        return call.call();
                    } catch (RuntimeException refused) {
                        return refused;
                    }
                }));
            }
            ready.await();
            start.countDown();
            List<Object> results = new ArrayList<>();
            for (Future<Object> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private static long count(List<Object> results, Class<?> type) {
        return results.stream().filter(type::isInstance).count();
    }

    @Test
    void aPlan_shouldBeListedWithItsCostAndItsDelay_andTakeNothingFromTheStock() {
        // Arrange
        Long id = fertilizerWith50("Plan NPK listed", "KG");
        // Act: one late plan and one in ten days, more than the stock in all
        FertilizerPlanResponse late = plan(id, " fpa ", TODAY.minusDays(3), 30.0);
        FertilizerPlanResponse coming = plan(id, "FPA", TODAY.plusDays(10), 40.0);
        // Assert
        assertEquals("FPA", late.blockCode());
        assertEquals(FertilizerPlanStatus.PLANNED, late.status());
        assertTrue(late.overdue());
        assertEquals(-3, late.daysFromToday());
        assertEquals(500L, late.unitPriceXof());
        assertEquals(TODAY.minusDays(30), late.priceDate());
        assertEquals(15000L, late.plannedCostXof());
        assertFalse(coming.overdue());
        assertEquals(10, coming.daysFromToday());
        assertEquals(20000L, coming.plannedCostXof());
        assertEquals(50.0, fertilizerService.getFertilizerById(id).currentStock());

        // Every filter at once, then the dates alone: both run on the database of the test
        assertEquals(List.of(coming.id()), fertilizerPlanService
                .getAllPlans(id, FertilizerPlanStatus.PLANNED, null, "FPA", TODAY, TODAY.plusDays(10)).stream()
                .map(FertilizerPlanResponse::id).toList());
        assertEquals(List.of(late.id(), coming.id()), fertilizerPlanService
                .getAllPlans(id, null, null, null, null, null).stream()
                .map(FertilizerPlanResponse::id).toList());
        assertTrue(fertilizerPlanService.getAllPlans(id, FertilizerPlanStatus.DONE, null, null, null, null).isEmpty());
        assertTrue(fertilizerPlanService.getAllPlans(id, null, 7, null, null, null).isEmpty());
    }

    @Test
    void completingAPlan_shouldRecordTheApplication_linkIt_andTakeItFromTheStock() {
        // Arrange
        Long id = fertilizerWith50("Plan NPK completed", "KG");
        FertilizerPlanResponse planned = plan(id, "FPB", TODAY.plusDays(2), 30.0);
        // Act: less is applied than planned
        FertilizerPlanResponse done = fertilizerPlanService.completePlan(planned.id(), completion(25.5));
        // Assert
        assertEquals(FertilizerPlanStatus.DONE, done.status());
        assertFalse(done.overdue());
        assertNull(done.daysFromToday());
        assertEquals(TODAY, done.actualDate());
        assertEquals(25.5, done.actualQuantity());
        assertEquals(30.0, done.plannedQuantity());
        assertEquals(24.5, fertilizerService.getFertilizerById(id).currentStock());

        List<FertilizerMovementResponse> applications = fertilizerMovementService.getAllMovements(
                id, FertilizerMovementType.APPLICATION, null, null, null, null);
        assertEquals(1, applications.size());
        assertEquals(done.movementId(), applications.get(0).id());
        assertEquals("FPB", applications.get(0).blockCode());
        assertEquals("Team A", applications.get(0).applicator());
        assertEquals("around the tree base", applications.get(0).method());

        // The plan read again carries the same application
        FertilizerPlanResponse reread = reread(id, planned.id());
        assertEquals(done.movementId(), reread.movementId());
        assertEquals(25.5, reread.actualQuantity());
    }

    @Test
    void completingAPlan_shouldRecordNothingAndLeaveItPlanned_whenTheStockDoesNotCoverIt() {
        // Arrange
        Long id = fertilizerWith50("Plan NPK refused", "KG");
        FertilizerPlanResponse planned = plan(id, "FPC", TODAY, 60.0);
        // Act
        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> fertilizerPlanService.completePlan(planned.id(), completion(60.0)));
        // Assert
        assertEquals("Not enough stock of Plan NPK refused: 50 kg left, 60 kg requested", exception.getMessage());
        FertilizerPlanResponse reread = reread(id, planned.id());
        assertEquals(FertilizerPlanStatus.PLANNED, reread.status());
        assertNull(reread.movementId());
        assertEquals(50.0, fertilizerService.getFertilizerById(id).currentStock());
        assertTrue(fertilizerMovementService.getAllMovements(
                id, FertilizerMovementType.APPLICATION, null, null, null, null).isEmpty());

        // The same plan can still be done with a quantity the stock covers
        assertEquals(FertilizerPlanStatus.DONE,
                fertilizerPlanService.completePlan(planned.id(), completion(50.0)).status());
    }

    @Test
    void aDoneOrCancelledPlan_shouldRefuseToBeCompletedOrCancelledAgain() {
        // Arrange
        Long id = fertilizerWith50("Plan NPK final", "KG");
        FertilizerPlanResponse done = plan(id, "FPD", TODAY, 10.0);
        FertilizerPlanResponse cancelled = plan(id, "FPD", TODAY, 10.0);
        fertilizerPlanService.completePlan(done.id(), completion(10.0));
        // Act
        FertilizerPlanResponse afterCancel = fertilizerPlanService.cancelPlan(cancelled.id());
        // Assert
        assertEquals(FertilizerPlanStatus.CANCELLED, afterCancel.status());
        assertNull(afterCancel.movementId());
        assertEquals("Planned application with ID " + done.id() + " is already done: "
                        + "only a planned application can be marked as done or cancelled",
                assertThrows(ConflictException.class,
                        () -> fertilizerPlanService.cancelPlan(done.id())).getMessage());
        assertEquals("Planned application with ID " + cancelled.id() + " is already cancelled: "
                        + "only a planned application can be marked as done or cancelled",
                assertThrows(ConflictException.class,
                        () -> fertilizerPlanService.completePlan(cancelled.id(), completion(10.0))).getMessage());
        assertEquals(40.0, fertilizerService.getFertilizerById(id).currentStock());
    }

    @Test
    void twoCompletionsOfOnePlanSentTogether_shouldRecordOneApplicationOnly() throws Exception {
        for (int trial = 1; trial <= TRIALS; trial++) {
            // Arrange
            Long id = fertilizerWith50("Plan race NPK " + trial, "KG");
            Long planId = plan(id, "FPE", TODAY, 10.0).id();
            Callable<Object> complete = () -> fertilizerPlanService.completePlan(planId, completion(10.0));
            // Act
            List<Object> results = together(List.of(complete, complete));
            // Assert: the second one waits for the first, then finds the plan done
            assertEquals(1, count(results, FertilizerPlanResponse.class), "trial " + trial);
            assertEquals(1, count(results, ConflictException.class), "trial " + trial);
            assertEquals(40.0, fertilizerService.getFertilizerById(id).currentStock(), "trial " + trial);
            assertEquals(1, fertilizerMovementService.getAllMovements(
                    id, FertilizerMovementType.APPLICATION, null, null, null, null).size(), "trial " + trial);
        }
    }

    @Test
    void completionAndCancellationOfOnePlanSentTogether_shouldKeepOneOfThem() throws Exception {
        for (int trial = 1; trial <= TRIALS; trial++) {
            // Arrange
            Long id = fertilizerWith50("Plan race Urea " + trial, "KG");
            Long planId = plan(id, "FPF", TODAY, 10.0).id();
            // Act
            List<Object> results = together(List.of(
                    () -> fertilizerPlanService.completePlan(planId, completion(10.0)),
                    () -> fertilizerPlanService.cancelPlan(planId)));
            // Assert: the stock agrees with the state that won
            assertEquals(1, count(results, FertilizerPlanResponse.class), "trial " + trial);
            assertEquals(1, count(results, ConflictException.class), "trial " + trial);
            FertilizerPlanResponse reread = reread(id, planId);
            double expectedStock = reread.status() == FertilizerPlanStatus.DONE ? 40.0 : 50.0;
            assertEquals(expectedStock, fertilizerService.getFertilizerById(id).currentStock(), "trial " + trial);
        }
    }

    @Test
    void theComparison_shouldTotalPlannedAndAppliedOfABlock_inTheUnitOfTheFertilizer() {
        // Arrange: litres on a block of its own; 20 L done with 18 L, 10 L still planned,
        // 5 L cancelled, 2 L applied without a plan
        Long id = fertilizerWith50("Plan liquid compared", "L");
        FertilizerPlanResponse done = plan(id, "FPG", TODAY, 20.0);
        plan(id, "FPG", TODAY.plusDays(5), 10.0);
        FertilizerPlanResponse cancelled = plan(id, "FPG", TODAY.plusDays(6), 5.0);
        fertilizerPlanService.completePlan(done.id(), completion(18.0));
        fertilizerPlanService.cancelPlan(cancelled.id());
        fertilizerMovementService.recordApplication(id,
                new ApplicationRequest(TODAY.minusDays(1), 2.0, null, "FPG", "Team B", null));
        // Act: without dates, the current year; then a period that holds the plans only
        List<FertilizerPlanComparisonResponse.Row> year = fertilizerPlanService.getComparison(null, null).rows()
                .stream().filter(row -> row.blockCode().equals("FPG")).toList();
        FertilizerPlanComparisonResponse later =
                fertilizerPlanService.getComparison(TODAY.plusDays(1), TODAY.plusDays(30));
        // Assert
        if (TODAY.plusDays(6).getYear() == TODAY.getYear() && TODAY.minusDays(1).getYear() == TODAY.getYear()) {
            assertEquals(List.of(new FertilizerPlanComparisonResponse.Row("FPG", id, "Plan liquid compared",
                    FertilizerUnit.L, 30.0, 10.0, 20.0, 2.0, -10.0, -33.3, 15000L, year.get(0).plannedCostEur(),
                    10000L, year.get(0).appliedCostEur(), TODAY.minusDays(30))), year);
        }
        assertEquals(TODAY.plusDays(1), later.from());
        assertEquals(TODAY.plusDays(30), later.to());
        FertilizerPlanComparisonResponse.Row laterRow = later.rows().stream()
                .filter(row -> row.blockCode().equals("FPG")).findFirst().orElseThrow();
        assertEquals(10.0, laterRow.plannedQuantity());
        assertEquals(10.0, laterRow.remainingQuantity());
        assertEquals(0.0, laterRow.appliedQuantity());
        assertEquals(-100.0, laterRow.gapPercent());
    }

    @Test
    void theStockCheck_shouldWarnAboutTheFertilizerPlannedBeyondItsStock() {
        // Arrange: 50 kg in stock; 30 kg late and 40 kg in ten days count, 100 kg in sixty
        // days and a cancelled plan do not; another fertilizer is planned within its stock
        Long short50 = fertilizerWith50("Plan NPK short", "KG");
        Long covered = fertilizerWith50("Plan liquid covered", "L");
        plan(short50, "FPH", TODAY.minusDays(2), 30.0);
        plan(short50, "FPI", TODAY.plusDays(10), 40.0);
        plan(short50, "FPH", TODAY.plusDays(60), 100.0);
        fertilizerPlanService.cancelPlan(plan(short50, "FPH", TODAY.plusDays(1), 500.0).id());
        plan(covered, "FPH", TODAY.plusDays(3), 50.0);
        // Act
        FertilizerPlanStockCheckResponse inThirtyDays = fertilizerPlanService.getStockCheck(null);
        FertilizerPlanStockCheckResponse inNinetyDays = fertilizerPlanService.getStockCheck(90);
        // Assert
        assertEquals(30, inThirtyDays.days());
        assertEquals(TODAY.plusDays(30), inThirtyDays.until());
        assertEquals(List.of(new FertilizerPlanStockCheckResponse.Warning(short50, "Plan NPK short",
                        FertilizerUnit.KG, 70.0, 30.0, 50.0, 20.0)),
                inThirtyDays.warnings().stream().filter(warning -> warning.fertilizerId().equals(short50)).toList());
        assertTrue(inThirtyDays.warnings().stream().noneMatch(warning -> warning.fertilizerId().equals(covered)));
        assertEquals(List.of(new FertilizerPlanStockCheckResponse.Warning(short50, "Plan NPK short",
                        FertilizerUnit.KG, 170.0, 30.0, 50.0, 120.0)),
                inNinetyDays.warnings().stream().filter(warning -> warning.fertilizerId().equals(short50)).toList());
    }
}
