package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.UnavailabilityScope;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.UnavailabilityStatus;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterUnavailability;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/** Optional search filters go through {@code WaterUnavailabilityService} (Criteria, PostgreSQL-safe). */
public interface WaterUnavailabilityRepository extends JpaRepository<WaterUnavailability, Long>,
        JpaSpecificationExecutor<WaterUnavailability> {

    /** Periods of a scope overlapping [startAt, endAt[ whose status is not {@code excluded}. */
    @Query("select u from WaterUnavailability u where u.scope = :scope and u.scopeId = :scopeId "
            + "and u.status <> :excluded and u.startAt < :endAt and u.endAt > :startAt")
    List<WaterUnavailability> overlapsExcept(UnavailabilityScope scope, Long scopeId, Instant startAt, Instant endAt,
                                             UnavailabilityStatus excluded);

    /** Non-cancelled periods of a scope overlapping [startAt, endAt[. */
    default List<WaterUnavailability> overlaps(UnavailabilityScope scope, Long scopeId, Instant startAt, Instant endAt) {
        return overlapsExcept(scope, scopeId, startAt, endAt, UnavailabilityStatus.CANCELLED);
    }

    /** Periods in force at the given instant, every scope, whose status is not {@code excluded}. */
    @Query("select u from WaterUnavailability u where u.status <> :excluded "
            + "and u.startAt <= :at and u.endAt > :at order by u.startAt asc")
    List<WaterUnavailability> activeAtExcept(Instant at, UnavailabilityStatus excluded);

    /** Non-cancelled periods in force at the given instant, every scope. */
    default List<WaterUnavailability> activeAt(Instant at) {
        return activeAtExcept(at, UnavailabilityStatus.CANCELLED);
    }
}
