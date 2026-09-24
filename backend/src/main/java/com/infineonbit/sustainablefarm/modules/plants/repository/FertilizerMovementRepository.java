package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerMovement;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerMovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    /**
     * Returns the movements matching the given filters, with their fertilizer.
     *
     * <p>Every parameter is optional: a {@code null} parameter disables its own
     * filter, as in {@link HarvestRecordRepository#findByOptionalFilters}. The farm
     * and the block are those of an application; a purchase or a loss has neither,
     * so any farm or block filter leaves it out. Both dates are included; a
     * {@code from} after {@code to} matches nothing and is not an error.
     *
     * <p>The dates are cast in their {@code IS NULL} test, for the same reason as
     * in {@link HarvestRecordRepository#findByOptionalFilters}: without it,
     * PostgreSQL cannot type the parameter as soon as a date is given.
     *
     * @param fertilizerId fertilizer identifier, or {@code null} for every fertilizer
     * @param movementType movement type, or {@code null} for every type
     * @param farmId       farm identifier, or {@code null} to ignore the farm
     * @param blockCode    raw block value as stored (for example {@code "B"}),
     *                     or {@code null} to ignore the block
     * @param from         first movement date, included, or {@code null} for no lower bound
     * @param to           last movement date, included, or {@code null} for no upper bound
     * @return the matching movements, ordered by date then identifier
     */
    @Query("""
            SELECT m FROM FertilizerMovement m JOIN FETCH m.product p
            WHERE (:fertilizerId IS NULL OR p.id = :fertilizerId)
              AND (:movementType IS NULL OR m.movementType = :movementType)
              AND (:farmId IS NULL OR m.farmId = :farmId)
              AND (:blockCode IS NULL OR m.blockCode = :blockCode)
              AND (CAST(:from AS LocalDate) IS NULL OR m.movementDate >= :from)
              AND (CAST(:to AS LocalDate) IS NULL OR m.movementDate <= :to)
            ORDER BY m.movementDate ASC, m.id ASC
            """)
    List<FertilizerMovement> findByOptionalFilters(@Param("fertilizerId") Long fertilizerId,
                                                   @Param("movementType") FertilizerMovementType movementType,
                                                   @Param("farmId") Integer farmId,
                                                   @Param("blockCode") String blockCode,
                                                   @Param("from") LocalDate from,
                                                   @Param("to") LocalDate to);

    /** Total quantity of one movement type of one fertilizer, as returned by {@link #findMovementTotals}. */
    interface MovementTotal {

        Long getProductId();

        FertilizerMovementType getMovementType();

        BigDecimal getTotal();
    }
}
