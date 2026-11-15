package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.ApplicationRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.FertilizerPlanCompletionRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.FertilizerPlanRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerMovementResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerPlanComparisonResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerPlanResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerPlanStockCheckResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.CurrencyRate;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerMovement;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerMovementType;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerPlan;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerPlanStatus;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerProduct;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerType;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerUnit;
import com.infineonbit.sustainablefarm.modules.plants.exception.FertilizerNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.exception.FertilizerPlanNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.CurrencyRateRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.FertilizerMovementRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.FertilizerMovementRepository.MovementTotal;
import com.infineonbit.sustainablefarm.modules.plants.repository.FertilizerPlanRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.FertilizerProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FertilizerPlanServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 14);

    @Mock
    private FertilizerPlanRepository fertilizerPlanRepository;

    @Mock
    private FertilizerProductRepository fertilizerProductRepository;

    @Mock
    private FertilizerMovementRepository fertilizerMovementRepository;

    @Mock
    private CurrencyRateRepository currencyRateRepository;

    @Mock
    private FertilizerMovementService fertilizerMovementService;

    private FertilizerPlanService fertilizerPlanService;

    @BeforeEach
    void createService() {
        fertilizerPlanService = new FertilizerPlanService(fertilizerPlanRepository, fertilizerProductRepository,
                fertilizerMovementRepository, currencyRateRepository, fertilizerMovementService,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static FertilizerProduct npk() {
        return new FertilizerProduct(1L, "NPK 15-15-15", FertilizerType.MINERAL, "15-15-15", FertilizerUnit.KG,
                new BigDecimal("50"), "user_entry", NOW);
    }

    private static FertilizerProduct liquid() {
        return new FertilizerProduct(2L, "Liquid feed", FertilizerType.ORGANIC, null, FertilizerUnit.L,
                null, "user_entry", NOW);
    }

    private static FertilizerPlan plan(long id, FertilizerProduct product, String block, LocalDate date,
                                       String quantity, FertilizerPlanStatus status) {
        return new FertilizerPlan(id, product, null, block, date, new BigDecimal(quantity), status, null, null,
                "user_entry", NOW);
    }

    private static FertilizerMovement movement(long id, FertilizerProduct product, FertilizerMovementType type,
                                               LocalDate date, String quantity) {
        FertilizerMovement movement = new FertilizerMovement();
        movement.setId(id);
        movement.setProduct(product);
        movement.setMovementType(type);
        movement.setMovementDate(date);
        movement.setQuantity(new BigDecimal(quantity));
        return movement;
    }

    /** The last priced purchase of NPK: 100,000 FCFA for 200 kg on 3 November, so 500 FCFA per kg. */
    private void npkLastBoughtAt500FcfaPerKg() {
        FertilizerMovement purchase = movement(10L, npk(), FertilizerMovementType.PURCHASE,
                LocalDate.of(2026, 11, 3), "200.000");
        purchase.setTotalCost(new BigDecimal("100000.00"));
        purchase.setCurrency("XOF");
        when(fertilizerMovementRepository.findPricedPurchases(Set.of(1L))).thenReturn(List.of(purchase));
    }

    private void rateIs(String rate) {
        when(currencyRateRepository.findByBaseCurrencyAndQuoteCurrency("EUR", "XOF")).thenReturn(Optional.of(
                new CurrencyRate(1L, "EUR", "XOF", new BigDecimal(rate), "BCEAO_fixed_parity_1999", NOW)));
    }

    /** Saving a plan gives it an identifier, as the database would. */
    private void planSaveAssignsId(long id) {
        when(fertilizerPlanRepository.save(any(FertilizerPlan.class))).thenAnswer(invocation -> {
            FertilizerPlan plan = invocation.getArgument(0);
            plan.setId(id);
            return plan;
        });
    }

    private static MovementTotal total(long productId, FertilizerMovementType type, String total) {
        return new MovementTotal() {
            @Override
            public Long getProductId() {
                return productId;
            }

            @Override
            public FertilizerMovementType getMovementType() {
                return type;
            }

            @Override
            public BigDecimal getTotal() {
                return new BigDecimal(total);
            }
        };
    }

    private static FertilizerPlanCompletionRequest completion(double quantity) {
        return new FertilizerPlanCompletionRequest(TODAY, quantity, " Team A ", null);
    }

    @Test
    void createPlan_shouldStoreAPlannedApplication_withItsCostAtTheLastPurchasePrice() {
        // Arrange
        when(fertilizerProductRepository.findById(1L)).thenReturn(Optional.of(npk()));
        npkLastBoughtAt500FcfaPerKg();
        rateIs("655.957");
        planSaveAssignsId(5L);
        // Act
        FertilizerPlanResponse response = fertilizerPlanService.createPlan(
                new FertilizerPlanRequest(1L, null, " b ", TODAY.plusDays(10), 30.0, "  before the rains  "),
                NOW, TODAY);
        // Assert: the block and the note are normalized, and nothing touches the stock
        ArgumentCaptor<FertilizerPlan> captor = ArgumentCaptor.forClass(FertilizerPlan.class);
        verify(fertilizerPlanRepository).save(captor.capture());
        FertilizerPlan stored = captor.getValue();
        assertEquals("B", stored.getBlockCode());
        assertEquals(new BigDecimal("30.0"), stored.getPlannedQuantity());
        assertEquals(FertilizerPlanStatus.PLANNED, stored.getStatus());
        assertEquals("before the rains", stored.getNote());
        assertEquals("user_entry", stored.getSource());
        assertEquals(NOW, stored.getLastUpdated());
        assertNull(stored.getMovement());
        verifyNoInteractions(fertilizerMovementService);

        assertEquals(new FertilizerPlanResponse(5L, 1L, "NPK 15-15-15", FertilizerUnit.KG, null, "B",
                TODAY.plusDays(10), 30.0, FertilizerPlanStatus.PLANNED, false, 10, 500L, 0.76,
                LocalDate.of(2026, 11, 3), 15000L, 22.87, null, null, null, "before the rains", "user_entry", NOW),
                response);
    }

    @Test
    void createPlan_shouldGiveNoCost_whenNoPurchaseOfTheFertilizerHasACost() {
        // Arrange
        when(fertilizerProductRepository.findById(2L)).thenReturn(Optional.of(liquid()));
        when(fertilizerMovementRepository.findPricedPurchases(Set.of(2L))).thenReturn(List.of());
        planSaveAssignsId(6L);
        // Act
        FertilizerPlanResponse response = fertilizerPlanService.createPlan(
                new FertilizerPlanRequest(2L, 1, "A", TODAY, 4.5, "   "), NOW, TODAY);
        // Assert: the unit is the fertilizer's, a blank note is not stored, and the rate is not read
        assertEquals(FertilizerUnit.L, response.unit());
        assertEquals(4.5, response.plannedQuantity());
        assertEquals(1, response.farmId());
        assertEquals(0, response.daysFromToday());
        assertFalse(response.overdue());
        assertNull(response.unitPriceXof());
        assertNull(response.unitPriceEur());
        assertNull(response.priceDate());
        assertNull(response.plannedCostXof());
        assertNull(response.plannedCostEur());
        assertNull(response.note());
        verifyNoInteractions(currencyRateRepository);
    }

    @Test
    void createPlan_shouldThrowNotFound_whenTheFertilizerIsUnknown() {
        // Arrange
        when(fertilizerProductRepository.findById(9L)).thenReturn(Optional.empty());
        // Act
        FertilizerNotFoundException exception = assertThrows(FertilizerNotFoundException.class,
                () -> fertilizerPlanService.createPlan(
                        new FertilizerPlanRequest(9L, null, "A", TODAY, 10.0, null), NOW, TODAY));
        // Assert
        assertEquals("Fertilizer with ID 9 not found", exception.getMessage());
        verify(fertilizerPlanRepository, never()).save(any());
    }

    @Test
    void getAllPlans_shouldFlagALatePlan_andGiveTheRealApplicationOfADoneOne() {
        // Arrange
        FertilizerPlan late = plan(1L, npk(), "A", TODAY.minusDays(3), "40.000", FertilizerPlanStatus.PLANNED);
        FertilizerPlan done = plan(2L, npk(), "B", TODAY.minusDays(5), "20.000", FertilizerPlanStatus.DONE);
        done.setMovement(movement(77L, npk(), FertilizerMovementType.APPLICATION, TODAY.minusDays(4), "18.500"));
        FertilizerPlan cancelled = plan(3L, npk(), "B", TODAY.minusDays(1), "5.000", FertilizerPlanStatus.CANCELLED);
        when(fertilizerPlanRepository.findByOptionalFilters(1L, null, null, "A", null, null))
                .thenReturn(List.of(done, late, cancelled));
        npkLastBoughtAt500FcfaPerKg();
        rateIs("655.957");
        // Act: a blank-padded block filter is trimmed
        List<FertilizerPlanResponse> responses =
                fertilizerPlanService.getAllPlans(1L, null, null, " A ", null, null, TODAY);
        // Assert: the order of the repository is kept
        assertEquals(3, responses.size());
        FertilizerPlanResponse doneResponse = responses.get(0);
        assertFalse(doneResponse.overdue());
        assertNull(doneResponse.daysFromToday());
        assertEquals(77L, doneResponse.movementId());
        assertEquals(TODAY.minusDays(4), doneResponse.actualDate());
        assertEquals(18.5, doneResponse.actualQuantity());
        assertEquals(10000L, doneResponse.plannedCostXof());
        FertilizerPlanResponse lateResponse = responses.get(1);
        assertTrue(lateResponse.overdue());
        assertEquals(-3, lateResponse.daysFromToday());
        assertNull(lateResponse.movementId());
        assertEquals(20000L, lateResponse.plannedCostXof());
        assertEquals(30.49, lateResponse.plannedCostEur());
        FertilizerPlanResponse cancelledResponse = responses.get(2);
        assertFalse(cancelledResponse.overdue());
        assertNull(cancelledResponse.daysFromToday());
    }

    @Test
    void getAllPlans_shouldReadNoPrice_whenNothingMatches() {
        // Arrange
        when(fertilizerPlanRepository.findByOptionalFilters(null, FertilizerPlanStatus.DONE, 2, null,
                TODAY, TODAY.plusDays(1))).thenReturn(List.of());
        // Act
        List<FertilizerPlanResponse> responses = fertilizerPlanService.getAllPlans(
                null, FertilizerPlanStatus.DONE, 2, "  ", TODAY, TODAY.plusDays(1), TODAY);
        // Assert
        assertTrue(responses.isEmpty());
        verifyNoInteractions(fertilizerMovementRepository, currencyRateRepository);
    }

    @Test
    void completePlan_shouldRecordTheRealApplication_andLinkItToThePlan() {
        // Arrange
        FertilizerPlan plan = plan(5L, npk(), "B", TODAY.minusDays(2), "30.000", FertilizerPlanStatus.PLANNED);
        plan.setFarmId(3);
        when(fertilizerPlanRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(plan));
        when(fertilizerMovementService.recordApplication(eq(1L), any(ApplicationRequest.class))).thenReturn(
                new FertilizerMovementResponse(77L, 1L, "NPK 15-15-15", FertilizerMovementType.APPLICATION, TODAY,
                        25.5, FertilizerUnit.KG, 3, "B", "Team A", null, null, null, null, null, null, null,
                        "user_entry", NOW));
        when(fertilizerMovementRepository.getReferenceById(77L)).thenReturn(
                movement(77L, npk(), FertilizerMovementType.APPLICATION, TODAY, "25.500"));
        when(fertilizerMovementRepository.findPricedPurchases(Set.of(1L))).thenReturn(List.of());
        planSaveAssignsId(5L);
        // Act
        FertilizerPlanResponse response = fertilizerPlanService.completePlan(5L, completion(25.5), NOW, TODAY);
        // Assert: the application is on the fertilizer, the farm and the block of the plan
        ArgumentCaptor<ApplicationRequest> captor = ArgumentCaptor.forClass(ApplicationRequest.class);
        verify(fertilizerMovementService).recordApplication(eq(1L), captor.capture());
        assertEquals(new ApplicationRequest(TODAY, 25.5, 3, "B", " Team A ", null), captor.getValue());
        assertEquals(FertilizerPlanStatus.DONE, plan.getStatus());
        assertEquals(NOW, plan.getLastUpdated());
        assertEquals(FertilizerPlanStatus.DONE, response.status());
        assertFalse(response.overdue());
        assertNull(response.daysFromToday());
        assertEquals(77L, response.movementId());
        assertEquals(TODAY, response.actualDate());
        assertEquals(25.5, response.actualQuantity());
        assertEquals(30.0, response.plannedQuantity());
    }

    @Test
    void completePlan_shouldLeaveThePlanPlanned_whenTheStockDoesNotCoverTheApplication() {
        // Arrange
        FertilizerPlan plan = plan(5L, npk(), "B", TODAY, "30.000", FertilizerPlanStatus.PLANNED);
        when(fertilizerPlanRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(plan));
        when(fertilizerMovementService.recordApplication(eq(1L), any(ApplicationRequest.class))).thenThrow(
                new BusinessRuleException("Not enough stock of NPK 15-15-15: 20 kg left, 30 kg requested"));
        // Act
        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> fertilizerPlanService.completePlan(5L, completion(30.0), NOW, TODAY));
        // Assert
        assertEquals("Not enough stock of NPK 15-15-15: 20 kg left, 30 kg requested", exception.getMessage());
        assertEquals(FertilizerPlanStatus.PLANNED, plan.getStatus());
        assertNull(plan.getMovement());
        verify(fertilizerPlanRepository, never()).save(any());
    }

    @Test
    void completePlan_shouldThrowConflict_whenThePlanIsAlreadyDone() {
        // Arrange
        when(fertilizerPlanRepository.findByIdForUpdate(5L)).thenReturn(
                Optional.of(plan(5L, npk(), "B", TODAY, "30.000", FertilizerPlanStatus.DONE)));
        // Act
        ConflictException exception = assertThrows(ConflictException.class,
                () -> fertilizerPlanService.completePlan(5L, completion(30.0), NOW, TODAY));
        // Assert: no second application is recorded
        assertEquals("Planned application with ID 5 is already done: "
                + "only a planned application can be marked as done or cancelled", exception.getMessage());
        verifyNoInteractions(fertilizerMovementService);
    }

    @Test
    void completePlan_shouldThrowNotFound_whenThePlanIsUnknown() {
        // Arrange
        when(fertilizerPlanRepository.findByIdForUpdate(9L)).thenReturn(Optional.empty());
        // Act
        FertilizerPlanNotFoundException exception = assertThrows(FertilizerPlanNotFoundException.class,
                () -> fertilizerPlanService.completePlan(9L, completion(30.0), NOW, TODAY));
        // Assert
        assertEquals("Planned application with ID 9 not found", exception.getMessage());
        verifyNoInteractions(fertilizerMovementService);
    }

    @Test
    void cancelPlan_shouldMarkThePlanCancelled_withoutTouchingTheStock() {
        // Arrange
        FertilizerPlan plan = plan(5L, npk(), "B", TODAY.minusDays(2), "30.000", FertilizerPlanStatus.PLANNED);
        when(fertilizerPlanRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(plan));
        when(fertilizerMovementRepository.findPricedPurchases(Set.of(1L))).thenReturn(List.of());
        planSaveAssignsId(5L);
        // Act
        FertilizerPlanResponse response = fertilizerPlanService.cancelPlan(5L, NOW, TODAY);
        // Assert: a cancelled plan is no longer late
        assertEquals(FertilizerPlanStatus.CANCELLED, response.status());
        assertFalse(response.overdue());
        assertNull(response.daysFromToday());
        assertNull(response.movementId());
        assertEquals(NOW, response.lastUpdated());
        verifyNoInteractions(fertilizerMovementService);
    }

    @Test
    void cancelPlan_shouldThrowConflict_whenThePlanIsAlreadyCancelled() {
        // Arrange
        when(fertilizerPlanRepository.findByIdForUpdate(5L)).thenReturn(
                Optional.of(plan(5L, npk(), "B", TODAY, "30.000", FertilizerPlanStatus.CANCELLED)));
        // Act
        ConflictException exception = assertThrows(ConflictException.class,
                () -> fertilizerPlanService.cancelPlan(5L, NOW, TODAY));
        // Assert
        assertEquals("Planned application with ID 5 is already cancelled: "
                + "only a planned application can be marked as done or cancelled", exception.getMessage());
        verify(fertilizerPlanRepository, never()).save(any());
    }

    @Test
    void getComparison_shouldUseTheCurrentYear_whenNoDateIsGiven() {
        // Arrange
        LocalDate first = LocalDate.of(2026, 1, 1);
        LocalDate last = LocalDate.of(2026, 12, 31);
        FertilizerProduct npk = npk();
        FertilizerMovement applied = application(101L, npk, "A", "35.000");
        FertilizerPlan done = plan(1L, npk, "A", TODAY.minusDays(20), "40.000", FertilizerPlanStatus.DONE);
        done.setMovement(applied);
        FertilizerPlan open = plan(2L, npk, "A", TODAY.plusDays(5), "20.000", FertilizerPlanStatus.PLANNED);
        when(fertilizerPlanRepository.findByOptionalFilters(null, null, null, null, first, last))
                .thenReturn(List.of(done, open));
        when(fertilizerMovementRepository.findByOptionalFilters(null, FertilizerMovementType.APPLICATION, null, null,
                first, last)).thenReturn(List.of(
                applied,
                application(102L, npk, "A", "5.000")));
        when(fertilizerPlanRepository.findLinkedMovementIds()).thenReturn(List.of(101L));
        npkLastBoughtAt500FcfaPerKg();
        rateIs("655.957");
        // Act
        FertilizerPlanComparisonResponse response = fertilizerPlanService.getComparison(null, null, TODAY);
        // Assert: 60 kg planned, 40 kg applied of which 5 kg without a plan
        assertEquals(first, response.from());
        assertEquals(last, response.to());
        assertEquals(List.of(new FertilizerPlanComparisonResponse.Row("A", 1L, "NPK 15-15-15", FertilizerUnit.KG,
                60.0, 20.0, 40.0, 5.0, -20.0, -33.3, 30000L, 45.73, 20000L, 30.49, LocalDate.of(2026, 11, 3))),
                response.rows());
    }

    @Test
    void getComparison_shouldKeepAnOpenSide_whenOneDateOnlyIsGiven() {
        // Arrange
        LocalDate from = LocalDate.of(2026, 6, 1);
        when(fertilizerPlanRepository.findByOptionalFilters(null, null, null, null, from, null))
                .thenReturn(List.of());
        when(fertilizerMovementRepository.findByOptionalFilters(null, FertilizerMovementType.APPLICATION, null, null,
                from, null)).thenReturn(List.of());
        when(fertilizerPlanRepository.findLinkedMovementIds()).thenReturn(List.of());
        // Act
        FertilizerPlanComparisonResponse response = fertilizerPlanService.getComparison(from, null, TODAY);
        // Assert
        assertEquals(from, response.from());
        assertNull(response.to());
        assertTrue(response.rows().isEmpty());
        verifyNoInteractions(currencyRateRepository);
    }

    @Test
    void getComparison_shouldGiveNoGapPercentAndNoCost_whenNothingWasPlannedAndNoPriceExists() {
        // Arrange
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);
        when(fertilizerPlanRepository.findByOptionalFilters(null, null, null, null, from, to)).thenReturn(List.of());
        when(fertilizerMovementRepository.findByOptionalFilters(null, FertilizerMovementType.APPLICATION, null, null,
                from, to)).thenReturn(List.of(application(103L, liquid(), "B", "2.500")));
        when(fertilizerPlanRepository.findLinkedMovementIds()).thenReturn(List.of());
        when(fertilizerMovementRepository.findPricedPurchases(Set.of(2L))).thenReturn(List.of());
        // Act
        FertilizerPlanComparisonResponse response = fertilizerPlanService.getComparison(from, to, TODAY);
        // Assert
        assertEquals(List.of(new FertilizerPlanComparisonResponse.Row("B", 2L, "Liquid feed", FertilizerUnit.L,
                0.0, 0.0, 2.5, 2.5, 2.5, null, null, null, null, null, null)), response.rows());
    }

    @Test
    void getStockCheck_shouldWarn_whenThePlansOfTheNext30DaysExceedTheStock() {
        // Arrange: 70 kg planned, 30 of them late, for 80 bought and 30 applied
        when(fertilizerPlanRepository.findOpenUntil(TODAY.plusDays(30))).thenReturn(List.of(
                plan(1L, npk(), "A", TODAY.minusDays(2), "30.000", FertilizerPlanStatus.PLANNED),
                plan(2L, npk(), "B", TODAY.plusDays(10), "40.000", FertilizerPlanStatus.PLANNED)));
        when(fertilizerMovementRepository.findMovementTotals(Set.of(1L))).thenReturn(List.of(
                total(1L, FertilizerMovementType.PURCHASE, "80.000"),
                total(1L, FertilizerMovementType.APPLICATION, "30.000")));
        // Act
        FertilizerPlanStockCheckResponse response = fertilizerPlanService.getStockCheck(null, TODAY);
        // Assert
        assertEquals(new FertilizerPlanStockCheckResponse(30, TODAY, TODAY.plusDays(30), List.of(
                new FertilizerPlanStockCheckResponse.Warning(1L, "NPK 15-15-15", FertilizerUnit.KG,
                        70.0, 30.0, 50.0, 20.0))), response);
    }

    @Test
    void getStockCheck_shouldGiveNoWarning_whenNothingIsPlannedInTheWindow() {
        // Arrange
        when(fertilizerPlanRepository.findOpenUntil(TODAY.plusDays(7))).thenReturn(List.of());
        // Act
        FertilizerPlanStockCheckResponse response = fertilizerPlanService.getStockCheck(7, TODAY);
        // Assert: the stocks are not read
        assertEquals(new FertilizerPlanStockCheckResponse(7, TODAY, TODAY.plusDays(7), List.of()), response);
        verifyNoInteractions(fertilizerMovementRepository);
    }

    private static FertilizerMovement application(long id, FertilizerProduct product, String block, String quantity) {
        FertilizerMovement application = movement(id, product, FertilizerMovementType.APPLICATION, TODAY, quantity);
        application.setBlockCode(block);
        return application;
    }
}
