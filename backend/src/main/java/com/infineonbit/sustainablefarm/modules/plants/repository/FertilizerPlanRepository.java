package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerPlan;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerPlanStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface FertilizerPlanRepository extends JpaRepository<FertilizerPlan, Long> {

    /**
     * Returns the planned applications matching the given filters, each with
     * its fertilizer and, once done, its real application.
     *
     * <p>Every parameter is optional: a {@code null} parameter disables its own
     * filter, as in {@link FertilizerMovementRepository#findByOptionalFilters}.
     * Both dates are included and compare the planned date; a {@code from}
     * after {@code to} matches nothing and is not an error. Each parameter is
     * cast in its {@code IS NULL} test for the same reason as there: without
     * it, PostgreSQL cannot type the parameter.
     *
     * @param fertilizerId fertilizer identifier, or {@code null} for every fertilizer
     * @param status       plan status, or {@code null} for every status
     * @param allBlocks    {@code true} for every block, then {@code blockIds} is ignored
     * @param blockIds     ids of the blocks to keep, never empty ({@code service.PlantsBlocks.BlockFilter})
     * @param from         first planned date, included, or {@code null} for no lower bound
     * @param to           last planned date, included, or {@code null} for no upper bound
     * @return the matching planned applications, ordered by planned date then identifier
     */
    @Query("""
            SELECT p FROM FertilizerPlan p JOIN FETCH p.product f LEFT JOIN FETCH p.movement
            WHERE (CAST(:fertilizerId AS Long) IS NULL OR f.id = :fertilizerId)
              AND (CAST(:status AS String) IS NULL OR p.status = :status)
              AND (:allBlocks = true OR p.blockId IN :blockIds)
              AND (CAST(:from AS LocalDate) IS NULL OR p.plannedDate >= :from)
              AND (CAST(:to AS LocalDate) IS NULL OR p.plannedDate <= :to)
            ORDER BY p.plannedDate ASC, p.id ASC
            """)
    List<FertilizerPlan> findByOptionalFilters(@Param("fertilizerId") Long fertilizerId,
                                               @Param("status") FertilizerPlanStatus status,
                                               @Param("allBlocks") boolean allBlocks,
                                               @Param("blockIds") Collection<Long> blockIds,
                                               @Param("from") LocalDate from,
                                               @Param("to") LocalDate to);

    /**
     * Returns the applications still PLANNED up to a date, late ones included,
     * each with its fertilizer: what the stock has to cover by then.
     *
     * @param until last planned date, included
     * @return the open planned applications, ordered by planned date then identifier
     */
    @Query("""
            SELECT p FROM FertilizerPlan p JOIN FETCH p.product
            WHERE p.status = com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerPlanStatus.PLANNED
              AND p.plannedDate <= :until
            ORDER BY p.plannedDate ASC, p.id ASC
            """)
    List<FertilizerPlan> findOpenUntil(@Param("until") LocalDate until);

    /**
     * Identifiers of the real applications that complete a planned one. An
     * application absent from this list was made without being planned.
     */
    @Query("SELECT p.movement.id FROM FertilizerPlan p WHERE p.movement IS NOT NULL")
    List<Long> findLinkedMovementIds();

    /**
     * Returns a planned application and locks its row until the end of the
     * transaction, so two requests that complete or cancel the same plan run
     * one after the other and the second one sees the state left by the first.
     * Must run inside a transaction.
     *
     * <p>No fetch join here: PostgreSQL refuses to lock the nullable side of
     * an outer join.
     *
     * @param id the planned application identifier
     * @return the locked planned application, or empty if none has this identifier
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM FertilizerPlan p WHERE p.id = :id")
    Optional<FertilizerPlan> findByIdForUpdate(@Param("id") Long id);
}
