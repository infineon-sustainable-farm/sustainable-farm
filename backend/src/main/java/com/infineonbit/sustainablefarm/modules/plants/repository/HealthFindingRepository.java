package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.HealthFinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
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
     * inspection, and each parameter is cast in its {@code IS NULL} test for
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
            WHERE (CAST(:farmId AS Integer) IS NULL OR i.farmId = :farmId)
              AND (CAST(:blockCode AS String) IS NULL OR i.blockCode = :blockCode)
              AND (CAST(:from AS LocalDate) IS NULL OR i.inspectedOn >= :from)
              AND (CAST(:to AS LocalDate) IS NULL OR i.inspectedOn <= :to)
            ORDER BY i.inspectedOn ASC, i.id ASC, f.id ASC
            """)
    List<HealthFinding> findByOptionalFilters(@Param("farmId") Integer farmId,
                                              @Param("blockCode") String blockCode,
                                              @Param("from") LocalDate from,
                                              @Param("to") LocalDate to);

    /**
     * Writes the resolution of a finding, only if it has none yet.
     *
     * <p>The condition sits in the update itself, so two resolutions sent at the
     * same time cannot both be written: the database runs the updates of one row
     * one after the other, and the second finds the resolution already there and
     * touches no row. The persistence context is cleared afterwards, so the
     * finding read next comes from the database.
     *
     * @param id         the finding
     * @param resolvedOn the resolution date
     * @param note       the resolution note, or {@code null}
     * @param now        the write time, stored as {@code lastUpdated}
     * @return 1 when the resolution was written, 0 when the finding was already resolved
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE HealthFinding f
            SET f.resolvedOn = :resolvedOn, f.resolutionNote = :note, f.lastUpdated = :now
            WHERE f.id = :id AND f.resolvedOn IS NULL
            """)
    int resolve(@Param("id") Long id,
                @Param("resolvedOn") LocalDate resolvedOn,
                @Param("note") String note,
                @Param("now") Instant now);
}
