package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryEvent;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface NurseryEventRepository extends JpaRepository<NurseryEvent, Long> {

    /**
     * The events of the given batches. One query serves a whole list of
     * batches; their counts, survival rate and stage are computed from them.
     *
     * @param batchIds identifiers of the batches, not empty
     * @return their events, ordered by date then identifier
     */
    @Query("""
            SELECT e FROM NurseryEvent e
            WHERE e.batch.id IN :batchIds
            ORDER BY e.eventDate ASC, e.id ASC
            """)
    List<NurseryEvent> findByBatchIds(@Param("batchIds") Collection<Long> batchIds);

    /**
     * Returns the events matching the given filters, each with its batch: the
     * history of the nursery.
     *
     * <p>Every parameter is optional: a {@code null} parameter disables its own
     * filter, as in {@link HarvestRecordRepository#findByOptionalFilters}; a
     * missing farm means every farm. The farm is that of the batch. Both dates
     * are included; a {@code from} after {@code to} matches nothing and is not
     * an error. Each parameter is cast in its {@code IS NULL} test, for the same
     * reason as in that method.
     *
     * @param batchId   batch identifier, or {@code null} for every batch
     * @param farmId    farm identifier, or {@code null} for every farm
     * @param eventType event type, or {@code null} for every type
     * @param from      first event date, included, or {@code null} for no lower bound
     * @param to        last event date, included, or {@code null} for no upper bound
     * @return the matching events, ordered by date then identifier
     */
    @Query("""
            SELECT e FROM NurseryEvent e JOIN FETCH e.batch b
            WHERE (CAST(:batchId AS Long) IS NULL OR b.id = :batchId)
              AND (CAST(:farmId AS Integer) IS NULL OR b.farmId = :farmId)
              AND (CAST(:eventType AS String) IS NULL OR e.eventType = :eventType)
              AND (CAST(:from AS LocalDate) IS NULL OR e.eventDate >= :from)
              AND (CAST(:to AS LocalDate) IS NULL OR e.eventDate <= :to)
            ORDER BY e.eventDate ASC, e.id ASC
            """)
    List<NurseryEvent> findByOptionalFilters(@Param("batchId") Long batchId,
                                             @Param("farmId") Integer farmId,
                                             @Param("eventType") NurseryEventType eventType,
                                             @Param("from") LocalDate from,
                                             @Param("to") LocalDate to);
}
