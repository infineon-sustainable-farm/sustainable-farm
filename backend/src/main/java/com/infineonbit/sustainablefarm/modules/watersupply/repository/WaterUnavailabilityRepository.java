package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterUnavailability;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/** Optional search filters go through {@code WaterUnavailabilityService} (Criteria, PostgreSQL-safe). */
public interface WaterUnavailabilityRepository extends JpaRepository<WaterUnavailability, UUID>,
        JpaSpecificationExecutor<WaterUnavailability> {

    /** Non-cancelled periods of a scope overlapping [startAt, endAt[. */
    @Query("select u from WaterUnavailability u where u.scope = :scope and u.scopeId = :scopeId "
            + "and lower(u.status) <> 'cancelled' and u.startAt < :endAt and u.endAt > :startAt")
    List<WaterUnavailability> overlaps(String scope, UUID scopeId, Instant startAt, Instant endAt);

    /** Non-cancelled periods in force at the given instant, every scope. */
    @Query("select u from WaterUnavailability u where lower(u.status) <> 'cancelled' "
            + "and u.startAt <= :at and u.endAt > :at order by u.startAt asc")
    List<WaterUnavailability> activeAt(Instant at);
}
