package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.HealthInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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
     * <p>The dates are cast in their {@code IS NULL} test, for the same reason as
     * in {@link HarvestRecordRepository#findByOptionalFilters}: without it,
     * PostgreSQL cannot type the parameter as soon as a date is given.
     *
     * @param farmId    farm identifier, or {@code null} for every farm
     * @param blockCode raw block value as stored (for example {@code "C"}),
     *                  or {@code null} for every block
     * @param from      first inspection date, included, or {@code null} for no lower bound
     * @param to        last inspection date, included, or {@code null} for no upper bound
     * @return the matching inspections, ordered by date then identifier
     */
    @Query("""
            SELECT i FROM HealthInspection i
            WHERE (:farmId IS NULL OR i.farmId = :farmId)
              AND (:blockCode IS NULL OR i.blockCode = :blockCode)
              AND (CAST(:from AS LocalDate) IS NULL OR i.inspectedOn >= :from)
              AND (CAST(:to AS LocalDate) IS NULL OR i.inspectedOn <= :to)
            ORDER BY i.inspectedOn ASC, i.id ASC
            """)
    List<HealthInspection> findByOptionalFilters(@Param("farmId") Integer farmId,
                                                 @Param("blockCode") String blockCode,
                                                 @Param("from") LocalDate from,
                                                 @Param("to") LocalDate to);
}
