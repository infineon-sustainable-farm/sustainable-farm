package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.HealthFinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface HealthFindingRepository extends JpaRepository<HealthFinding, Long> {

    /**
     * The findings of the given inspections, with their inspection and their
     * issue. One query serves a whole list of inspections.
     *
     * @param inspectionIds identifiers of the inspections, not empty
     * @return their findings, ordered by inspection then identifier
     */
    @Query("""
            SELECT f FROM HealthFinding f JOIN FETCH f.inspection i JOIN FETCH f.issue
            WHERE i.id IN :inspectionIds
            ORDER BY i.id ASC, f.id ASC
            """)
    List<HealthFinding> findByInspectionIds(@Param("inspectionIds") Collection<Long> inspectionIds);

    /**
     * Returns the findings whose inspection matches the given filters, with
     * their inspection and their issue.
     *
     * <p>Every parameter is optional, as in
     * {@link HealthInspectionRepository#findByOptionalFilters}: a missing farm
     * means every farm, both dates are included and are those of the
     * inspection, and the dates are cast in their {@code IS NULL} test for
     * PostgreSQL. The status is computed afterwards, so it is filtered in Java.
     *
     * @param farmId    farm identifier, or {@code null} for every farm
     * @param blockCode raw block value as stored (for example {@code "C"}),
     *                  or {@code null} for every block
     * @param from      first inspection date, included, or {@code null} for no lower bound
     * @param to        last inspection date, included, or {@code null} for no upper bound
     * @return the matching findings, ordered by inspection date, inspection, then identifier
     */
    @Query("""
            SELECT f FROM HealthFinding f JOIN FETCH f.inspection i JOIN FETCH f.issue
            WHERE (:farmId IS NULL OR i.farmId = :farmId)
              AND (:blockCode IS NULL OR i.blockCode = :blockCode)
              AND (CAST(:from AS LocalDate) IS NULL OR i.inspectedOn >= :from)
              AND (CAST(:to AS LocalDate) IS NULL OR i.inspectedOn <= :to)
            ORDER BY i.inspectedOn ASC, i.id ASC, f.id ASC
            """)
    List<HealthFinding> findByOptionalFilters(@Param("farmId") Integer farmId,
                                              @Param("blockCode") String blockCode,
                                              @Param("from") LocalDate from,
                                              @Param("to") LocalDate to);
}
