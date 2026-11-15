package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerMovementType;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerUnit;
import com.infineonbit.sustainablefarm.modules.plants.repository.FertilizerMovementRepository.MovementTotal;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stock arithmetic of the fertilizers, with no data of its own.
 *
 * <p>The stock of a fertilizer is PURCHASE + ADJUSTMENT_IN - APPLICATION - LOSS
 * - ADJUSTMENT_OUT, computed from the totals of its movements. A fertilizer
 * without any movement has a stock of 0: what was never bought cannot be applied.
 */
final class FertilizerStockCalculator {

    private FertilizerStockCalculator() {
    }

    /**
     * A quantity with the sign of its movement type: positive when it enters the
     * stock, negative when it leaves it.
     *
     * <p>The switch has no default, so a new movement type does not compile until
     * it says which way it moves the stock.
     *
     * @param type     the movement type
     * @param quantity the quantity moved, positive
     * @return the quantity, negated for the types that take from the stock
     */
    static BigDecimal signedQuantity(FertilizerMovementType type, BigDecimal quantity) {
        return switch (type) {
            case PURCHASE, ADJUSTMENT_IN -> quantity;
            case APPLICATION, LOSS, ADJUSTMENT_OUT -> quantity.negate();
        };
    }

    /**
     * The stock of each fertilizer that has movements.
     *
     * @param totals the totals by fertilizer and movement type
     * @return the stock by fertilizer identifier; a fertilizer without movement is absent
     */
    static Map<Long, BigDecimal> stocksByProduct(List<MovementTotal> totals) {
        Map<Long, BigDecimal> stocks = new HashMap<>();
        for (MovementTotal total : totals) {
            stocks.merge(total.getProductId(),
                    signedQuantity(total.getMovementType(), total.getTotal()),
                    BigDecimal::add);
        }
        return stocks;
    }

    /**
     * The stock of one fertilizer.
     *
     * @param productId the fertilizer identifier
     * @param totals    the totals of its movements, by type
     * @return its stock, 0 when it has no movement
     */
    static BigDecimal stockOf(Long productId, List<MovementTotal> totals) {
        return stocksByProduct(totals).getOrDefault(productId, BigDecimal.ZERO);
    }

    /**
     * Whether a fertilizer should be reordered.
     *
     * @param stock     its current stock
     * @param threshold its reorder threshold, or {@code null} when none was entered
     * @return {@code true} when a threshold exists and the stock is at or below it
     */
    static boolean belowThreshold(BigDecimal stock, BigDecimal threshold) {
        return threshold != null && stock.compareTo(threshold) <= 0;
    }

    /**
     * A quantity as written in a message: without trailing zeros and never in
     * scientific notation, so 50.000 is {@code "50"} and 12.50 is {@code "12.5"}.
     */
    static String plain(BigDecimal quantity) {
        return quantity.stripTrailingZeros().toPlainString();
    }

    /**
     * Refusal of an application or a loss larger than the stock, for example
     * "Not enough stock of NPK 15-15-15: 50 kg left, 60 kg requested".
     *
     * @param name      the fertilizer name
     * @param stock     its current stock
     * @param requested the quantity of the refused movement
     * @param unit      the unit of the fertilizer
     * @return the message of the 422
     */
    static String notEnoughStockMessage(String name, BigDecimal stock, BigDecimal requested, FertilizerUnit unit) {
        return "Not enough stock of " + name + ": "
                + plain(stock) + " " + unit.getSymbol() + " left, "
                + plain(requested) + " " + unit.getSymbol() + " requested";
    }
}
