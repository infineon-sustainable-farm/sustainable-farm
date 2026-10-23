package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.RainwaterHarvest;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RainwaterHarvestRepository extends JpaRepository<RainwaterHarvest, UUID> {
    Page<RainwaterHarvest> findBySourceId(UUID sourceId, Pageable pageable);

    /**
     * Rainwater volume recovered over a semi-open interval [start, end[: computed in the
     * database instead of a full table scan on the application side.
     */
    @Query("select coalesce(sum(h.harvestedLiters), 0) from RainwaterHarvest h "
            + "where h.captureDate >= :start and h.captureDate < :end")
    double sumHarvestedLitersBetween(Instant start, Instant end);
}
