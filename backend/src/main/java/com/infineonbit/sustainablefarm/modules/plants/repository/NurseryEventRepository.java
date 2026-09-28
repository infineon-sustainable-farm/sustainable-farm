package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
