package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerMovementType;
import com.infineonbit.sustainablefarm.modules.plants.repository.FertilizerMovementRepository.MovementTotal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FertilizerStockCalculatorTest {

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

    /** Decimals are compared by value: 300.000 and 300 are the same stock. */
    private static void assertDecimal(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), () -> expected + " expected, got " + actual);
    }

    @Test
    void signedQuantity_shouldAddPurchasesAndInAdjustments_andSubtractTheOthers() {
        // Arrange
        BigDecimal ten = new BigDecimal("10");
        // Act & Assert
        assertDecimal("10", FertilizerStockCalculator.signedQuantity(FertilizerMovementType.PURCHASE, ten));
        assertDecimal("10", FertilizerStockCalculator.signedQuantity(FertilizerMovementType.ADJUSTMENT_IN, ten));
        assertDecimal("-10", FertilizerStockCalculator.signedQuantity(FertilizerMovementType.APPLICATION, ten));
        assertDecimal("-10", FertilizerStockCalculator.signedQuantity(FertilizerMovementType.LOSS, ten));
        assertDecimal("-10", FertilizerStockCalculator.signedQuantity(FertilizerMovementType.ADJUSTMENT_OUT, ten));
    }

    @Test
    void stocksByProduct_shouldCombineEveryTypeOfEachFertilizer() {
        // Arrange: NPK bought 300, applied 250, lost 10, adjusted +5 then -2; compost bought 40
        List<MovementTotal> totals = List.of(
                total(1L, FertilizerMovementType.PURCHASE, "300.000"),
                total(1L, FertilizerMovementType.APPLICATION, "250.000"),
                total(1L, FertilizerMovementType.LOSS, "10.000"),
                total(1L, FertilizerMovementType.ADJUSTMENT_IN, "5.000"),
                total(1L, FertilizerMovementType.ADJUSTMENT_OUT, "2.000"),
                total(2L, FertilizerMovementType.PURCHASE, "40.000"));
        // Act
        Map<Long, BigDecimal> stocks = FertilizerStockCalculator.stocksByProduct(totals);
        // Assert
        assertEquals(2, stocks.size());
        assertDecimal("43", stocks.get(1L));
        assertDecimal("40", stocks.get(2L));
    }

    @Test
    void stockOf_shouldBeExact_forDecimalsThatDriftAsDoubles() {
        // Arrange: 0.3 bought, then 0.1 and 0.2 applied, summed by the database
        List<MovementTotal> totals = List.of(
                total(1L, FertilizerMovementType.PURCHASE, "0.300"),
                total(1L, FertilizerMovementType.APPLICATION, "0.300"));
        // Act & Assert: exactly 0, where doubles give -2.7755575615628914E-17
        assertEquals(0, FertilizerStockCalculator.stockOf(1L, totals).signum());
    }

    @Test
    void stockOf_shouldBeZero_whenFertilizerHasNoMovement() {
        // Act & Assert
        assertDecimal("0", FertilizerStockCalculator.stockOf(7L, List.of()));
        assertDecimal("0", FertilizerStockCalculator.stockOf(7L,
                List.of(total(1L, FertilizerMovementType.PURCHASE, "50"))));
    }

    @Test
    void belowThreshold_shouldBeTrue_whenStockIsAtOrBelowTheThreshold() {
        // Act & Assert
        assertTrue(FertilizerStockCalculator.belowThreshold(new BigDecimal("50.000"), new BigDecimal("50")));
        assertTrue(FertilizerStockCalculator.belowThreshold(new BigDecimal("49.999"), new BigDecimal("50")));
        assertTrue(FertilizerStockCalculator.belowThreshold(BigDecimal.ZERO, BigDecimal.ZERO));
        assertFalse(FertilizerStockCalculator.belowThreshold(new BigDecimal("50.001"), new BigDecimal("50")));
    }

    @Test
    void belowThreshold_shouldBeFalse_whenThereIsNoThreshold() {
        // Act & Assert: even an empty stock is not flagged without a threshold
        assertFalse(FertilizerStockCalculator.belowThreshold(BigDecimal.ZERO, null));
    }
}
