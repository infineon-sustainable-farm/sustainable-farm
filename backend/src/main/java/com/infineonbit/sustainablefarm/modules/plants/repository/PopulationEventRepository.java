package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEvent;
import com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PopulationEventRepository extends JpaRepository<PopulationEvent, Long> {

    /**
     * Current number of trees of each given variety row, computed from its events.
     *
     * <p>The balance is the sum of PLANTING, REPLACEMENT and EXTENSION minus the
     * sum of MORTALITY and REMOVAL. It is computed on every read and never
     * stored. One query serves a whole list of varieties.
     *
     * <p>A variety row with no event has no line in the result: its current
     * number of trees is unknown, not zero.
     *
     * @param varietyIds identifiers of the variety rows, not empty
     * @return one balance per variety row that has at least one event
     */
    @Query("""
            SELECT e.variety.id AS varietyId,
                   SUM(CASE
                           WHEN e.eventType IN (
                                   com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEventType.PLANTING,
                                   com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEventType.REPLACEMENT,
                                   com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEventType.EXTENSION)
                               THEN e.treeCount
                           WHEN e.eventType IN (
                                   com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEventType.MORTALITY,
                                   com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEventType.REMOVAL)
                               THEN -e.treeCount
                           ELSE 0
                       END) AS balance
            FROM PopulationEvent e
            WHERE e.variety.id IN :varietyIds
            GROUP BY e.variety.id
            """)
    List<TreeBalance> findTreeBalances(@Param("varietyIds") Collection<Long> varietyIds);

    /**
     * Whether the variety row already has an event of this type. Used with
     * PLANTING: a variety row is planted once.
     */
    boolean existsByVarietyIdAndEventType(Long varietyId, PopulationEventType eventType);

    /**
     * The oldest event of this type of the variety row. Used with PLANTING to
     * read the planting date of a variety, which is the date of its event and
     * not the calendar date of its block.
     *
     * @param varietyId identifier of the variety row
     * @param eventType the event type, PLANTING for the planting date
     * @return the oldest such event, or empty if the variety row has none
     */
    Optional<PopulationEvent> findFirstByVarietyIdAndEventTypeOrderByEventDateAsc(Long varietyId,
                                                                                 PopulationEventType eventType);

    /** Current number of trees of one variety row, as returned by {@link #findTreeBalances}. */
    interface TreeBalance {

        Long getVarietyId();

        Long getBalance();
    }
}
