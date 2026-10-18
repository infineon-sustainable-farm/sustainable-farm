package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationSchedule;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IrrigationScheduleRepository extends JpaRepository<IrrigationSchedule, UUID> {
    Page<IrrigationSchedule> findByZoneId(UUID zoneId, Pageable pageable);

    List<IrrigationSchedule> findByZoneId(UUID zoneId);

    long countByStartTimeBetween(Instant start, Instant end);

    /**
     * Volume avoided by the postponed irrigations over a semi-open interval [start, end[:
     * this is the savings obtained by not irrigating when the forecast rain covered the need.
     */
    @Query("select coalesce(sum(s.waterQuantityLiters), 0) from IrrigationSchedule s "
            + "where lower(s.status) = 'postponed' and s.startTime >= :start and s.startTime < :end")
    double sumPostponedLitersBetween(Instant start, Instant end);
}
