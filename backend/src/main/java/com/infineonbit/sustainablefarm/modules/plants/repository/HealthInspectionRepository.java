package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.HealthInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface HealthInspectionRepository extends JpaRepository<HealthInspection, Long> {

    /**
     * Returns the inspections matching the given filters.
     *
     * <p>Every parameter is optional: a {@code null} parameter disables its own
     * filter, as in {@link HarvestRecordRepository#findByOptionalFilters}; a
     * missing farm means every farm. Both dates are included; a {@code from}
     * after {@code to} matches nothing and is not an error.
     *
     * <p>Each parameter is cast in its {@code IS NULL} test, for the same reason as
     * in {@link HarvestRecordRepository#findByOptionalFilters}: without it,
     * PostgreSQL cannot type the parameter as soon as a date is given.
     *
     * @param allBlocks {@code true} for every block, then {@code blockIds} is ignored
     * @param blockIds  ids of the blocks to keep, never empty ({@code service.PlantsBlocks.BlockFilter})
     * @param from      first inspection date, included, or {@code null} for no lower bound
     * @param to        last inspection date, included, or {@code null} for no upper bound
     * @return the matching inspections, ordered by date then identifier
     */
    @Query("""
            SELECT i FROM HealthInspection i
            WHERE (:allBlocks = true OR i.blockId IN :blockIds)
              AND (CAST(:from AS LocalDate) IS NULL OR i.inspectedOn >= :from)
              AND (CAST(:to AS LocalDate) IS NULL OR i.inspectedOn <= :to)
            ORDER BY i.inspectedOn ASC, i.id ASC
            """)
    List<HealthInspection> findByOptionalFilters(@Param("allBlocks") boolean allBlocks,
                                                 @Param("blockIds") Collection<Long> blockIds,
                                                 @Param("from") LocalDate from,
                                                 @Param("to") LocalDate to);
}
