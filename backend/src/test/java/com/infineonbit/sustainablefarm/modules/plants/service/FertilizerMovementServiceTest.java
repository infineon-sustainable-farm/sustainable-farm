package com.infineonbit.sustainablefarm.modules.plants.service;

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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

    /** Saving the movement gives it an identifier, as the database would. */
    private void movementSaveAssignsId(long id) {
        when(fertilizerMovementRepository.save(any(FertilizerMovement.class))).thenAnswer(invocation -> {
            FertilizerMovement movement = invocation.getArgument(0);
            movement.setId(id);
            return movement;
        });
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
    void recordPurchase_shouldFail_whenACostCannotBeConvertedForLackOfRate() {
        // Arrange
        npkInCatalogue();
        when(currencyRateRepository.findByBaseCurrencyAndQuoteCurrency("EUR", "XOF")).thenReturn(Optional.empty());
        // Act & Assert: answered with a 500, the purchase is not stored
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> fertilizerMovementService.recordPurchase(1L,
                        new PurchaseRequest(JUNE_1, 200.0, "Supplier A", 150000.0, null), NOW));
        assertEquals("No EUR to XOF rate in currency_rate", exception.getMessage());
        verify(fertilizerMovementRepository, never()).save(any());
    }
}
