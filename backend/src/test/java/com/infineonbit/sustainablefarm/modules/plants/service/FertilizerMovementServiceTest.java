package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.ApplicationRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.LossRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PurchaseRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerMovementResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.CurrencyRate;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerMovement;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerMovementType;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerProduct;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerType;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerUnit;
import com.infineonbit.sustainablefarm.modules.plants.exception.FertilizerNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.CurrencyRateRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.FertilizerMovementRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.FertilizerMovementRepository.MovementTotal;
import com.infineonbit.sustainablefarm.modules.plants.repository.FertilizerProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FertilizerMovementServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-24T10:00:00Z");
    private static final LocalDate JUNE_1 = LocalDate.of(2026, 6, 1);

    @Mock
    private FertilizerProductRepository fertilizerProductRepository;

    @Mock
    private FertilizerMovementRepository fertilizerMovementRepository;

    @Mock
    private CurrencyRateRepository currencyRateRepository;

    @InjectMocks
    private FertilizerMovementService fertilizerMovementService;

    private static FertilizerProduct npk() {
        return new FertilizerProduct(1L, "NPK 15-15-15", FertilizerType.MINERAL, "15-15-15", FertilizerUnit.KG,
                new BigDecimal("50"), "user_entry", NOW);
    }

    private FertilizerProduct npkInCatalogue() {
        FertilizerProduct npk = npk();
        when(fertilizerProductRepository.findById(1L)).thenReturn(Optional.of(npk));
        return npk;
    }

    private void rateIs(String rate) {
        when(currencyRateRepository.findByBaseCurrencyAndQuoteCurrency("EUR", "XOF")).thenReturn(Optional.of(
                new CurrencyRate(1L, "EUR", "XOF", new BigDecimal(rate), "BCEAO_fixed_parity_1999", NOW)));
    }

    /** No rate entered yet: the state of every environment but dev, until the user enters one. */
    private void noRate() {
        when(currencyRateRepository.findByBaseCurrencyAndQuoteCurrency("EUR", "XOF")).thenReturn(Optional.empty());
    }

    /** Saving the movement gives it an identifier, as the database would. */
    private void movementSaveAssignsId(long id) {
        when(fertilizerMovementRepository.save(any(FertilizerMovement.class))).thenAnswer(invocation -> {
            FertilizerMovement movement = invocation.getArgument(0);
            movement.setId(id);
            return movement;
        });
    }

    private static MovementTotal total(FertilizerMovementType type, String total) {
        return new MovementTotal() {
            @Override
            public Long getProductId() {
                return 1L;
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

    /** The NPK movements so far, as totals by type, as the database sums them. */
    private void npkTotalsAre(MovementTotal... totals) {
        when(fertilizerMovementRepository.findMovementTotals(List.of(1L))).thenReturn(List.of(totals));
    }

    private static ApplicationRequest application(double quantity) {
        return new ApplicationRequest(LocalDate.of(2026, 6, 15), quantity, null, " b ", " Team A ",
                " around the tree base ");
    }

    private static FertilizerMovement storedPurchase(long id, LocalDate date, String cost, String currency) {
        FertilizerMovement purchase = new FertilizerMovement();
        purchase.setId(id);
        purchase.setProduct(npk());
        purchase.setMovementType(FertilizerMovementType.PURCHASE);
        purchase.setMovementDate(date);
        purchase.setQuantity(new BigDecimal("100.000"));
        purchase.setSupplier("Supplier A");
        purchase.setTotalCost(cost == null ? null : new BigDecimal(cost));
        purchase.setCurrency(currency);
        purchase.setSource("user_entry");
        purchase.setLastUpdated(NOW);
        return purchase;
    }

    private FertilizerMovement savedMovement() {
        ArgumentCaptor<FertilizerMovement> captor = ArgumentCaptor.forClass(FertilizerMovement.class);
        verify(fertilizerMovementRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void recordPurchase_shouldStoreThePurchase_inXofWhenNoCurrencyIsGiven() {
        // Arrange
        FertilizerProduct npk = npkInCatalogue();
        rateIs("655.957");
        movementSaveAssignsId(10L);
        // Act
        FertilizerMovementResponse response = fertilizerMovementService.recordPurchase(1L,
                new PurchaseRequest(JUNE_1, 200.0, " Supplier A ", 150000.0, null), NOW);
        // Assert: the stored row, with only the purchase columns set
        FertilizerMovement saved = savedMovement();
        assertSame(npk, saved.getProduct());
        assertEquals(FertilizerMovementType.PURCHASE, saved.getMovementType());
        assertEquals(JUNE_1, saved.getMovementDate());
        assertEquals(0, new BigDecimal("200").compareTo(saved.getQuantity()));
        assertEquals("Supplier A", saved.getSupplier());
        assertEquals(0, new BigDecimal("150000").compareTo(saved.getTotalCost()));
        assertEquals("XOF", saved.getCurrency());
        assertNull(saved.getBlockCode());
        assertNull(saved.getApplicator());
        assertNull(saved.getReason());
        assertEquals("user_entry", saved.getSource());
        assertEquals(NOW, saved.getLastUpdated());
        // Assert: the cost in both currencies
        assertEquals(new FertilizerMovementResponse(10L, 1L, "NPK 15-15-15", FertilizerMovementType.PURCHASE, JUNE_1,
                200.0, FertilizerUnit.KG, null, null, null, null, "Supplier A", 150000.0, "XOF", 150000L, 228.67,
                null, "user_entry", NOW), response);
    }

    @Test
    void recordPurchase_shouldConvertACostInEuros_whateverTheCaseOfTheCurrency() {
        // Arrange
        npkInCatalogue();
        rateIs("655.957");
        movementSaveAssignsId(11L);
        // Act
        FertilizerMovementResponse response = fertilizerMovementService.recordPurchase(1L,
                new PurchaseRequest(LocalDate.of(2026, 6, 5), 100.0, "Supplier B", 120.0, " eur "), NOW);
        // Assert
        assertEquals("EUR", savedMovement().getCurrency());
        assertEquals("EUR", response.currency());
        assertEquals(120.0, response.totalCost());
        assertEquals(78715L, response.totalCostXof());
        assertEquals(120.0, response.totalCostEur());
    }

    @Test
    void recordPurchase_shouldIgnoreTheCurrency_whenThereIsNoCost() {
        // Arrange
        npkInCatalogue();
        movementSaveAssignsId(12L);
        // Act
        FertilizerMovementResponse response = fertilizerMovementService.recordPurchase(1L,
                new PurchaseRequest(JUNE_1, 25.0, "Supplier A", null, "EUR"), NOW);
        // Assert: nothing to convert, so the rate is not read
        assertNull(savedMovement().getCurrency());
        assertNull(response.totalCost());
        assertNull(response.currency());
        assertNull(response.totalCostXof());
        assertNull(response.totalCostEur());
        verifyNoInteractions(currencyRateRepository);
    }

    @Test
    void recordPurchase_shouldThrowNotFound_whenFertilizerIsUnknown() {
        // Arrange
        when(fertilizerProductRepository.findById(999L)).thenReturn(Optional.empty());
        // Act & Assert
        FertilizerNotFoundException exception = assertThrows(FertilizerNotFoundException.class,
                () -> fertilizerMovementService.recordPurchase(999L,
                        new PurchaseRequest(JUNE_1, 200.0, "Supplier A", null, null), NOW));
        assertEquals("Fertilizer with ID 999 not found", exception.getMessage());
        verify(fertilizerMovementRepository, never()).save(any());
    }

    @Test
    void recordPurchase_shouldStoreACostInXof_andLeaveItsEuroAmountEmpty_whenNoRateIsRecorded() {
        // Arrange
        npkInCatalogue();
        noRate();
        movementSaveAssignsId(13L);
        // Act
        FertilizerMovementResponse response = fertilizerMovementService.recordPurchase(1L,
                new PurchaseRequest(JUNE_1, 200.0, "Supplier A", 150000.0, null), NOW);
        // Assert: the purchase is stored as entered; only the amount that needs the rate is missing
        FertilizerMovement saved = savedMovement();
        assertEquals(0, new BigDecimal("150000").compareTo(saved.getTotalCost()));
        assertEquals("XOF", saved.getCurrency());
        assertEquals(150000.0, response.totalCost());
        assertEquals("XOF", response.currency());
        assertEquals(150000L, response.totalCostXof());
        assertNull(response.totalCostEur());
    }

    @Test
    void recordPurchase_shouldStoreACostInEuros_andLeaveItsFcfaAmountEmpty_whenNoRateIsRecorded() {
        // Arrange
        npkInCatalogue();
        noRate();
        movementSaveAssignsId(14L);
        // Act
        FertilizerMovementResponse response = fertilizerMovementService.recordPurchase(1L,
                new PurchaseRequest(JUNE_1, 100.0, "Supplier B", 120.0, "EUR"), NOW);
        // Assert
        assertEquals("EUR", savedMovement().getCurrency());
        assertEquals(120.0, response.totalCost());
        assertNull(response.totalCostXof());
        assertEquals(120.0, response.totalCostEur());
    }

    @Test
    void recordApplication_shouldStoreTheApplication_onABlockWithoutAnyPlanting() {
        // Arrange: 300 kg bought; nothing is planted anywhere, and the service
        // reads no variety nor planting: only the fertilizer and its movements
        FertilizerProduct npk = npkInCatalogue();
        npkTotalsAre(total(FertilizerMovementType.PURCHASE, "300.000"));
        movementSaveAssignsId(13L);
        // Act
        FertilizerMovementResponse response = fertilizerMovementService.recordApplication(1L, application(250.0), NOW);
        // Assert: the stored row, with only the application columns set
        FertilizerMovement saved = savedMovement();
        assertSame(npk, saved.getProduct());
        assertEquals(FertilizerMovementType.APPLICATION, saved.getMovementType());
        assertEquals(LocalDate.of(2026, 6, 15), saved.getMovementDate());
        assertEquals(0, new BigDecimal("250").compareTo(saved.getQuantity()));
        assertNull(saved.getFarmId());
        assertEquals("B", saved.getBlockCode());
        assertEquals("Team A", saved.getApplicator());
        assertEquals("around the tree base", saved.getMethod());
        assertNull(saved.getSupplier());
        assertNull(saved.getTotalCost());
        assertNull(saved.getCurrency());
        assertNull(saved.getReason());
        assertEquals("user_entry", saved.getSource());
        assertEquals(NOW, saved.getLastUpdated());
        assertEquals(new FertilizerMovementResponse(13L, 1L, "NPK 15-15-15", FertilizerMovementType.APPLICATION,
                LocalDate.of(2026, 6, 15), 250.0, FertilizerUnit.KG, null, "B", "Team A", "around the tree base",
                null, null, null, null, null, null, "user_entry", NOW), response);
        verifyNoInteractions(currencyRateRepository);
    }

    @Test
    void recordApplication_shouldStoreNoMethod_whenItIsBlank() {
        // Arrange
        npkInCatalogue();
        npkTotalsAre(total(FertilizerMovementType.PURCHASE, "300.000"));
        movementSaveAssignsId(14L);
        // Act
        fertilizerMovementService.recordApplication(1L,
                new ApplicationRequest(LocalDate.of(2026, 6, 15), 10.0, 1, "A", "Team A", "  "), NOW);
        // Assert
        FertilizerMovement saved = savedMovement();
        assertEquals(1, saved.getFarmId());
        assertNull(saved.getMethod());
    }

    @Test
    void recordApplication_shouldRefuseMoreThanTheStock_with422() {
        // Arrange: 300 kg bought, 250 kg applied, 50 kg left
        npkInCatalogue();
        npkTotalsAre(total(FertilizerMovementType.PURCHASE, "300.000"),
                total(FertilizerMovementType.APPLICATION, "250.000"));
        // Act & Assert
        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> fertilizerMovementService.recordApplication(1L, application(60.0), NOW));
        assertEquals("Not enough stock of NPK 15-15-15: 50 kg left, 60 kg requested", exception.getMessage());
        verify(fertilizerMovementRepository, never()).save(any());
    }

    @Test
    void recordApplication_shouldRefuseAFertilizerNeverBought() {
        // Arrange: no movement at all
        npkInCatalogue();
        npkTotalsAre();
        // Act & Assert
        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> fertilizerMovementService.recordApplication(1L, application(1.0), NOW));
        assertEquals("Not enough stock of NPK 15-15-15: 0 kg left, 1 kg requested", exception.getMessage());
    }

    @Test
    void recordApplication_shouldAcceptTheWholeStock_whenDecimalsWouldDriftAsDoubles() {
        // Arrange: 0.3 bought and 0.1 applied leave exactly 0.2, where doubles leave 0.19999999999999998
        npkInCatalogue();
        npkTotalsAre(total(FertilizerMovementType.PURCHASE, "0.300"),
                total(FertilizerMovementType.APPLICATION, "0.100"));
        movementSaveAssignsId(15L);
        // Act
        FertilizerMovementResponse response = fertilizerMovementService.recordApplication(1L, application(0.2), NOW);
        // Assert
        assertEquals(0.2, response.quantity());
    }

    @Test
    void recordApplication_shouldThrowNotFound_whenFertilizerIsUnknown() {
        // Arrange
        when(fertilizerProductRepository.findById(999L)).thenReturn(Optional.empty());
        // Act & Assert
        assertThrows(FertilizerNotFoundException.class,
                () -> fertilizerMovementService.recordApplication(999L, application(1.0), NOW));
        verify(fertilizerMovementRepository, never()).save(any());
    }

    @Test
    void recordLoss_shouldStoreTheLoss() {
        // Arrange: 50 kg left
        npkInCatalogue();
        npkTotalsAre(total(FertilizerMovementType.PURCHASE, "300.000"),
                total(FertilizerMovementType.APPLICATION, "250.000"));
        movementSaveAssignsId(16L);
        // Act
        FertilizerMovementResponse response = fertilizerMovementService.recordLoss(1L,
                new LossRequest(LocalDate.of(2026, 7, 1), 10.0, " expired "), NOW);
        // Assert: only the loss columns are set
        FertilizerMovement saved = savedMovement();
        assertEquals(FertilizerMovementType.LOSS, saved.getMovementType());
        assertEquals(LocalDate.of(2026, 7, 1), saved.getMovementDate());
        assertEquals(0, new BigDecimal("10").compareTo(saved.getQuantity()));
        assertEquals("expired", saved.getReason());
        assertNull(saved.getBlockCode());
        assertNull(saved.getApplicator());
        assertNull(saved.getSupplier());
        assertEquals(NOW, saved.getLastUpdated());
        assertEquals("expired", response.reason());
        assertEquals(10.0, response.quantity());
    }

    @Test
    void recordLoss_shouldRefuseMoreThanTheStock_with422() {
        // Arrange: 300 bought, 250 applied, 10 lost: 40 kg left
        npkInCatalogue();
        npkTotalsAre(total(FertilizerMovementType.PURCHASE, "300.000"),
                total(FertilizerMovementType.APPLICATION, "250.000"),
                total(FertilizerMovementType.LOSS, "10.000"));
        // Act & Assert
        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> fertilizerMovementService.recordLoss(1L,
                        new LossRequest(LocalDate.of(2026, 7, 2), 41.0, "expired"), NOW));
        assertEquals("Not enough stock of NPK 15-15-15: 40 kg left, 41 kg requested", exception.getMessage());
        verify(fertilizerMovementRepository, never()).save(any());
    }

    @Test
    void getAllMovements_shouldConvertEveryCost_readingTheRateOnce() {
        // Arrange
        rateIs("655.957");
        when(fertilizerMovementRepository.findByOptionalFilters(null, null, null, null, null, null)).thenReturn(List.of(
                storedPurchase(1L, JUNE_1, "150000.00", "XOF"),
                storedPurchase(2L, LocalDate.of(2026, 6, 5), "120.00", "EUR"),
                storedPurchase(3L, LocalDate.of(2026, 6, 6), null, null)));
        // Act
        List<FertilizerMovementResponse> responses =
                fertilizerMovementService.getAllMovements(null, null, null, null, null, null);
        // Assert
        assertEquals(3, responses.size());
        assertEquals(150000L, responses.get(0).totalCostXof());
        assertEquals(228.67, responses.get(0).totalCostEur());
        assertEquals(78715L, responses.get(1).totalCostXof());
        assertEquals(120.0, responses.get(1).totalCostEur());
        assertNull(responses.get(2).totalCostXof());
        assertNull(responses.get(2).totalCostEur());
        verify(currencyRateRepository, times(1)).findByBaseCurrencyAndQuoteCurrency("EUR", "XOF");
    }

    @Test
    void getAllMovements_shouldGiveEachCostInItsOwnCurrencyOnly_whenNoRateIsRecorded() {
        // Arrange
        noRate();
        when(fertilizerMovementRepository.findByOptionalFilters(null, null, null, null, null, null)).thenReturn(List.of(
                storedPurchase(1L, JUNE_1, "150000.00", "XOF"),
                storedPurchase(2L, LocalDate.of(2026, 6, 5), "120.00", "EUR")));
        // Act: the list is still served, with no error
        List<FertilizerMovementResponse> responses =
                fertilizerMovementService.getAllMovements(null, null, null, null, null, null);
        // Assert
        assertEquals(2, responses.size());
        assertEquals(150000L, responses.get(0).totalCostXof());
        assertNull(responses.get(0).totalCostEur());
        assertNull(responses.get(1).totalCostXof());
        assertEquals(120.0, responses.get(1).totalCostEur());
    }

    @Test
    void getAllMovements_shouldNotReadTheRate_whenNoMovementHasACost() {
        // Arrange
        when(fertilizerMovementRepository.findByOptionalFilters(null, null, null, null, null, null))
                .thenReturn(List.of(storedPurchase(3L, JUNE_1, null, null)));
        // Act
        fertilizerMovementService.getAllMovements(null, null, null, null, null, null);
        // Assert
        verifyNoInteractions(currencyRateRepository);
    }

    @Test
    void getAllMovements_shouldPassEveryFilter_withABlankBlockAsNoFilter() {
        // Arrange
        LocalDate from = LocalDate.of(2026, 6, 2);
        LocalDate to = LocalDate.of(2026, 6, 30);
        when(fertilizerMovementRepository.findByOptionalFilters(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        // Act
        fertilizerMovementService.getAllMovements(1L, FertilizerMovementType.APPLICATION, 2, " B ", from, to);
        fertilizerMovementService.getAllMovements(null, null, null, "  ", null, null);
        // Assert: the block is trimmed, a blank one is no filter
        verify(fertilizerMovementRepository).findByOptionalFilters(1L, FertilizerMovementType.APPLICATION, 2, "B",
                from, to);
        verify(fertilizerMovementRepository).findByOptionalFilters(null, null, null, null, null, null);
    }
}
