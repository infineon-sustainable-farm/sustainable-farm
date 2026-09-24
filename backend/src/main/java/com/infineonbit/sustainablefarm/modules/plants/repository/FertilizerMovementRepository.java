package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerMovement;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerMovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@Repository
public interface FertilizerMovementRepository extends JpaRepository<FertilizerMovement, Long> {

    /**
     * Total quantity of each movement type of each given fertilizer.
     *
     * <p>The stock is computed from these totals on every read and never stored:
     * the service adds the types that enter the stock and subtracts the others
     * (see {@code FertilizerStockCalculator}). The sum runs in SQL on exact
     * decimals, so it does not drift. One query serves a whole list of fertilizers.
     *
     * <p>A fertilizer with no movement has no line in the result: its stock is 0.
     *
     * @param productIds identifiers of the fertilizers, not empty
     * @return one total per fertilizer and movement type that has at least one movement
     */
    @Query("""
            SELECT m.product.id AS productId,
                   m.movementType AS movementType,
                   SUM(m.quantity) AS total
            FROM FertilizerMovement m
            WHERE m.product.id IN :productIds
            GROUP BY m.product.id, m.movementType
            """)
    List<MovementTotal> findMovementTotals(@Param("productIds") Collection<Long> productIds);

    /** Total quantity of one movement type of one fertilizer, as returned by {@link #findMovementTotals}. */
    interface MovementTotal {

        Long getProductId();

        FertilizerMovementType getMovementType();

        BigDecimal getTotal();
    }
}
