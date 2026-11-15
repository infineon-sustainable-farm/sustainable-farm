package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.MaturityObservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MaturityObservationRepository extends JpaRepository<MaturityObservation, Long> {

    /**
     * Returns the observations matching the given filters, with their variety
     * row.
     *
     * <p>Every parameter is optional: a {@code null} parameter disables its own
     * filter, as in {@link HarvestRecordRepository#findByOptionalFilters}. The
     * farm and the block are those of the variety row. Both dates are
     * included; a {@code from} after {@code to} matches nothing and is not an
     * error.
     *
     * <p>The parameters are cast in their {@code IS NULL} test. PostgreSQL
     * cannot infer the type of a parameter that appears only in
     * {@code ? IS NULL}, and its driver sends dates without a type: without
     * the cast, the query fails with "could not determine data type of
     * parameter" as soon as a date is given. H2 infers the type, so the tests
     * on H2 do not catch this case.
     *
     * <p>The variety is fetched with the observations: it is a lazy
     * association, and open-in-view is disabled.
     *
     * @param farmId    farm identifier, or {@code null} to ignore the farm
     * @param blockCode raw block value as stored (for example {@code "A"}),
     *                  or {@code null} to ignore the block
     * @param from      first observation date, included, or {@code null} for no lower bound
     * @param to        last observation date, included, or {@code null} for no upper bound
     * @return the matching observations, ordered by date then identifier
     */
    @Query("""
            SELECT o FROM MaturityObservation o JOIN FETCH o.variety v
            WHERE (CAST(:farmId AS Integer) IS NULL OR v.farmId = :farmId)
              AND (CAST(:blockCode AS String) IS NULL OR v.blockCode = :blockCode)
              AND (CAST(:from AS LocalDate) IS NULL OR o.observedOn >= :from)
              AND (CAST(:to AS LocalDate) IS NULL OR o.observedOn <= :to)
            ORDER BY o.observedOn ASC, o.id ASC
            """)
    List<MaturityObservation> findByOptionalFilters(@Param("farmId") Integer farmId,
                                                    @Param("blockCode") String blockCode,
                                                    @Param("from") LocalDate from,
                                                    @Param("to") LocalDate to);
}
