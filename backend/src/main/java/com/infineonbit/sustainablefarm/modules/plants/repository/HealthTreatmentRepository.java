package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.HealthTreatment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface HealthTreatmentRepository extends JpaRepository<HealthTreatment, Long> {

    /**
     * The treatments that answer the given findings. One query serves a whole
     * list of findings; the status and dates of each are computed from them.
     *
     * @param findingIds identifiers of the findings, not empty
     * @return their treatments, ordered by date then identifier
     */
    @Query("""
            SELECT t FROM HealthTreatment t
            WHERE t.finding.id IN :findingIds
            ORDER BY t.treatedOn ASC, t.id ASC
            """)
    List<HealthTreatment> findByFindingIds(@Param("findingIds") Collection<Long> findingIds);

    /**
     * Returns the treatments matching the given filters, with their targeted
     * issue: the treatment record.
     *
     * <p>Every parameter is optional: a {@code null} parameter disables its own
     * filter, as in {@link HarvestRecordRepository#findByOptionalFilters}; a
     * missing farm means every farm. Both dates are included; a {@code from}
     * after {@code to} matches nothing and is not an error. Each parameter is
     * cast in its {@code IS NULL} test, for the same reason as in that method.
     *
     * @param allBlocks {@code true} for every block, then {@code blockIds} is ignored
     * @param blockIds  ids of the blocks to keep, never empty ({@code service.PlantsBlocks.BlockFilter})
     * @param from      first treatment date, included, or {@code null} for no lower bound
     * @param to        last treatment date, included, or {@code null} for no upper bound
     * @return the matching treatments, ordered by date then identifier
     */
    @Query("""
            SELECT t FROM HealthTreatment t JOIN FETCH t.targetIssue
            WHERE (:allBlocks = true OR t.blockId IN :blockIds)
              AND (CAST(:from AS LocalDate) IS NULL OR t.treatedOn >= :from)
              AND (CAST(:to AS LocalDate) IS NULL OR t.treatedOn <= :to)
            ORDER BY t.treatedOn ASC, t.id ASC
            """)
    List<HealthTreatment> findByOptionalFilters(@Param("allBlocks") boolean allBlocks,
                                                @Param("blockIds") Collection<Long> blockIds,
                                                @Param("from") LocalDate from,
                                                @Param("to") LocalDate to);
}
