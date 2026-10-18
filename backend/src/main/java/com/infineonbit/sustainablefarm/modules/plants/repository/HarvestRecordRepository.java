package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.HarvestRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HarvestRecordRepository extends JpaRepository<HarvestRecord, Long> {

    /**
     * Returns the harvests matching the given filters, with their variety row.
     *
     * <p>Every parameter is optional: a {@code null} parameter disables its own
     * filter, as in {@link VarietyRepository#findByOptionalFilters}. The farm and
     * the block are those of the variety row. Both dates are included; a
     * {@code from} after {@code to} matches nothing and is not an error.
     *
     * <p>The dates are cast in their {@code IS NULL} test. PostgreSQL cannot infer
     * the type of a parameter that appears only in {@code ? IS NULL}, and its
     * driver sends dates without a type: without the cast, the query fails with
     * "could not determine data type of parameter" as soon as a date is given.
     * H2 infers the type, so the tests do not catch this case.
     *
     * @param farmId    farm identifier, or {@code null} to ignore the farm
     * @param blockCode raw block value as stored (for example {@code "A"}),
     *                  or {@code null} to ignore the block
     * @param from      first harvest date, included, or {@code null} for no lower bound
     * @param to        last harvest date, included, or {@code null} for no upper bound
     * @return the matching harvests, ordered by date then identifier
     */
    @Query("""
            SELECT h FROM HarvestRecord h JOIN FETCH h.variety v
            WHERE (CAST(:farmId AS Integer) IS NULL OR v.farmId = :farmId)
              AND (CAST(:blockCode AS String) IS NULL OR v.blockCode = :blockCode)
              AND (CAST(:from AS LocalDate) IS NULL OR h.harvestDate >= :from)
              AND (CAST(:to AS LocalDate) IS NULL OR h.harvestDate <= :to)
            ORDER BY h.harvestDate ASC, h.id ASC
            """)
    List<HarvestRecord> findByOptionalFilters(@Param("farmId") Integer farmId,
                                              @Param("blockCode") String blockCode,
                                              @Param("from") LocalDate from,
                                              @Param("to") LocalDate to);
}
