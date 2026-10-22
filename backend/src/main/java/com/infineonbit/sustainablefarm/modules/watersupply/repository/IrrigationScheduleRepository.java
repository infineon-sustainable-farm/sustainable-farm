package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationSchedule;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IrrigationScheduleRepository extends JpaRepository<IrrigationSchedule, UUID> {
    Page<IrrigationSchedule> findByZoneId(UUID zoneId, Pageable pageable);

    List<IrrigationSchedule> findByZoneId(UUID zoneId);

    List<IrrigationSchedule> findByZoneIdAndStatusIgnoreCase(UUID zoneId, String status);

    long countByStartTimeBetween(Instant start, Instant end);

    /**
     * Volume avoided by the postponed irrigations over a semi-open interval [start, end[:
     * this is the savings obtained by not irrigating when the forecast rain covered the need.
     */
    @Query("select coalesce(sum(s.waterQuantityLiters), 0) from IrrigationSchedule s "
            + "where lower(s.status) = 'postponed' and s.startTime >= :start and s.startTime < :end")
    double sumPostponedLitersBetween(Instant start, Instant end);

    /** Volume still to be delivered (scheduled or running irrigations) over [start, end[: water balance forecast. */
    @Query("select coalesce(sum(s.waterQuantityLiters), 0) from IrrigationSchedule s "
            + "where lower(s.status) in ('scheduled','running') and s.startTime >= :start and s.startTime < :end")
    double sumPlannedDemandBetween(Instant start, Instant end);

    /** Same forecast restricted to the zones of one farm (zone -> field -> farm). */
    @Query("select coalesce(sum(s.waterQuantityLiters), 0) from IrrigationSchedule s, Zone z, Field f "
            + "where s.zoneId = z.id and z.fieldId = f.id and f.farmId = :farmId "
            + "and lower(s.status) in ('scheduled','running') and s.startTime >= :start and s.startTime < :end")
    double sumPlannedDemandForFarmBetween(UUID farmId, Instant start, Instant end);

    Page<IrrigationSchedule> findByZoneIdIn(Collection<UUID> zoneIds, Pageable pageable);

    @Query("select s from IrrigationSchedule s where s.zoneId = :zoneId "
            + "and lower(s.status) in ('scheduled','running') and s.startTime >= :start and s.startTime < :end")
    List<IrrigationSchedule> findOpenForZoneBetween(UUID zoneId, Instant start, Instant end);
}
