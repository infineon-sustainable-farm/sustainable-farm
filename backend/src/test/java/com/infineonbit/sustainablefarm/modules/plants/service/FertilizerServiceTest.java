package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.FertilizerRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerMovementType;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerProduct;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerType;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerUnit;
import com.infineonbit.sustainablefarm.modules.plants.exception.FertilizerNotFoundException;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FertilizerServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-24T10:00:00Z");

    @Mock
    private FertilizerProductRepository fertilizerProductRepository;

    @Mock
    private FertilizerMovementRepository fertilizerMovementRepository;

    @InjectMocks
    private FertilizerService fertilizerService;

    private static FertilizerProduct product(Long id, String name, String threshold) {
        return new FertilizerProduct(id, name, FertilizerType.MINERAL, null, FertilizerUnit.KG,
                threshold == null ? null : new BigDecimal(threshold), "user_entry", NOW);
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

    /** Saving the fertilizer gives it an identifier, as the database would. */
    private void productSaveAssignsId(long id) {
        when(fertilizerProductRepository.save(any(FertilizerProduct.class))).thenAnswer(invocation -> {
            FertilizerProduct product = invocation.getArgument(0);
            product.setId(id);
            return product;
        });
    }

    @Test
    void createFertilizer_shouldStoreTheNormalizedFertilizer_withAStockOfZero() {
        // Arrange
        when(fertilizerProductRepository.findAll()).thenReturn(List.of());
        productSaveAssignsId(1L);
        // Act
        FertilizerResponse response = fertilizerService.createFertilizer(
                new FertilizerRequest("  NPK 15-15-15 ", " mineral ", " 15-15-15 ", "kg", 50.0), NOW);
        // Assert: the stored row, trimmed and with the codes upper-cased
        ArgumentCaptor<FertilizerProduct> captor = ArgumentCaptor.forClass(FertilizerProduct.class);
        verify(fertilizerProductRepository).save(captor.capture());
        FertilizerProduct saved = captor.getValue();
        assertEquals("NPK 15-15-15", saved.getName());
        assertEquals(FertilizerType.MINERAL, saved.getFertilizerType());
        assertEquals("15-15-15", saved.getComposition());
        assertEquals(FertilizerUnit.KG, saved.getUnit());
        assertEquals(0, new BigDecimal("50").compareTo(saved.getReorderThreshold()));
        assertEquals("user_entry", saved.getSource());
        assertEquals(NOW, saved.getLastUpdated());
        // Assert: a new fertilizer has no stock, so it is at its threshold
        assertEquals(new FertilizerResponse(1L, "NPK 15-15-15", FertilizerType.MINERAL, "15-15-15",
                FertilizerUnit.KG, 50.0, 0.0, true, "user_entry", NOW), response);
        verifyNoInteractions(fertilizerMovementRepository);
    }

    @Test
    void createFertilizer_shouldLeaveBelowThresholdFalse_whenThereIsNoThreshold() {
        // Arrange
        when(fertilizerProductRepository.findAll()).thenReturn(List.of());
        productSaveAssignsId(2L);
        // Act: a blank composition is no composition
        FertilizerResponse response = fertilizerService.createFertilizer(
                new FertilizerRequest("Compost", "ORGANIC", "   ", "KG", null), NOW);
        // Assert
        assertEquals(FertilizerType.ORGANIC, response.fertilizerType());
        assertNull(response.composition());
        assertNull(response.reorderThreshold());
        assertEquals(0.0, response.currentStock());
        assertFalse(response.belowThreshold());
    }

    @Test
    void createFertilizer_shouldRefuseADuplicate_ignoringCaseAccentsAndSurroundingSpaces() {
        // Arrange
        when(fertilizerProductRepository.findAll()).thenReturn(List.of(
                product(1L, "NPK 15-15-15", null), product(2L, "Urée", null)));
        // Act & Assert: the message names the fertilizer already in the catalogue
        ConflictException sameCase = assertThrows(ConflictException.class, () -> fertilizerService.createFertilizer(
                new FertilizerRequest("npk 15-15-15", "MINERAL", null, "KG", null), NOW));
        assertEquals("A fertilizer named NPK 15-15-15 already exists", sameCase.getMessage());
        ConflictException sameAccents = assertThrows(ConflictException.class, () -> fertilizerService.createFertilizer(
                new FertilizerRequest(" UREE ", "MINERAL", null, "KG", null), NOW));
        assertEquals("A fertilizer named Urée already exists", sameAccents.getMessage());
        verify(fertilizerProductRepository, never()).save(any());
    }

    @Test
    void createFertilizer_shouldAcceptANameThatDiffersByMoreThanCaseAndAccents() {
        // Arrange
        when(fertilizerProductRepository.findAll()).thenReturn(List.of(product(1L, "NPK 15-15-15", null)));
        productSaveAssignsId(2L);
        // Act
        FertilizerResponse response = fertilizerService.createFertilizer(
                new FertilizerRequest("NPK 20-10-10", "MINERAL", null, "KG", null), NOW);
        // Assert
        assertEquals("NPK 20-10-10", response.name());
    }

    @Test
    void getAllFertilizers_shouldGiveEachFertilizerItsStock_zeroWithoutMovement() {
        // Arrange: NPK at 50 with a threshold of 50, compost never bought
        when(fertilizerProductRepository.findAllByOrderByNameAsc()).thenReturn(List.of(
                product(2L, "Compost", null), product(1L, "NPK 15-15-15", "50")));
        when(fertilizerMovementRepository.findMovementTotals(List.of(2L, 1L))).thenReturn(List.of(
                total(1L, FertilizerMovementType.PURCHASE, "300.000"),
                total(1L, FertilizerMovementType.APPLICATION, "250.000")));
        // Act
        List<FertilizerResponse> responses = fertilizerService.getAllFertilizers();
        // Assert: the order of the repository is kept
        assertEquals(List.of("Compost", "NPK 15-15-15"), responses.stream().map(FertilizerResponse::name).toList());
        assertEquals(0.0, responses.get(0).currentStock());
        assertFalse(responses.get(0).belowThreshold());
        assertEquals(50.0, responses.get(1).currentStock());
        assertTrue(responses.get(1).belowThreshold());
    }

    @Test
    void getAllFertilizers_shouldNotQueryTheMovements_whenCatalogueIsEmpty() {
        // Arrange
        when(fertilizerProductRepository.findAllByOrderByNameAsc()).thenReturn(List.of());
        // Act & Assert
        assertTrue(fertilizerService.getAllFertilizers().isEmpty());
        verifyNoInteractions(fertilizerMovementRepository);
    }

    @Test
    void getFertilizerById_shouldReturnTheFertilizerWithItsStock() {
        // Arrange
        when(fertilizerProductRepository.findById(1L)).thenReturn(Optional.of(product(1L, "NPK 15-15-15", "50")));
        when(fertilizerMovementRepository.findMovementTotals(List.of(1L))).thenReturn(List.of(
                total(1L, FertilizerMovementType.PURCHASE, "300.000")));
        // Act
        FertilizerResponse response = fertilizerService.getFertilizerById(1L);
        // Assert
        assertEquals(300.0, response.currentStock());
        assertFalse(response.belowThreshold());
    }

    @Test
    void getFertilizerById_shouldThrowNotFound_whenIdIsUnknown() {
        // Arrange
        when(fertilizerProductRepository.findById(999L)).thenReturn(Optional.empty());
        // Act & Assert
        FertilizerNotFoundException exception = assertThrows(FertilizerNotFoundException.class,
                () -> fertilizerService.getFertilizerById(999L));
        assertEquals("Fertilizer with ID 999 not found", exception.getMessage());
        verifyNoInteractions(fertilizerMovementRepository);
    }
}
