package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationSchedule;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IrrigationScheduleRepository extends JpaRepository<IrrigationSchedule, Long> {
    Page<IrrigationSchedule> findByZoneId(Long zoneId, Pageable pageable);

    List<IrrigationSchedule> findByZoneId(Long zoneId);

    List<IrrigationSchedule> findByZoneIdAndStatus(Long zoneId, IrrigationStatus status);

    long countByStartTimeBetween(Instant start, Instant end);

    /**
     * Volume avoided by the postponed irrigations over a semi-open interval [start, end[:
     * this is the savings obtained by not irrigating when the forecast rain covered the need.
     */
    default double sumPostponedLitersBetween(Instant start, Instant end) {
        return sumLitersBetween(List.of(IrrigationStatus.POSTPONED), start, end);
    }

    /** Volume of the irrigations in one of the given statuses over [start, end[. */
    @Query("select coalesce(sum(s.waterQuantityLiters), 0) from IrrigationSchedule s "
            + "where s.status in :statuses and s.startTime >= :start and s.startTime < :end")
    double sumLitersBetween(Collection<IrrigationStatus> statuses, Instant start, Instant end);

    /** Volume still to be delivered (scheduled or running irrigations) over [start, end[: water balance forecast. */
    default double sumPlannedDemandBetween(Instant start, Instant end) {
        return sumLitersBetween(IrrigationStatus.OPEN, start, end);
    }

    /** Same forecast restricted to some zones (the zones of one farm, read from the farm referential). */
    default double sumPlannedDemandForZonesBetween(Collection<Long> zoneIds, Instant start, Instant end) {
        return sumLitersForZonesBetween(zoneIds, IrrigationStatus.OPEN, start, end);
    }

    /** Volume of the irrigations of some zones in one of the given statuses over [start, end[. */
    @Query("select coalesce(sum(s.waterQuantityLiters), 0) from IrrigationSchedule s where s.zoneId in :zoneIds "
            + "and s.status in :statuses and s.startTime >= :start and s.startTime < :end")
    double sumLitersForZonesBetween(Collection<Long> zoneIds, Collection<IrrigationStatus> statuses,
                                    Instant start, Instant end);

    Page<IrrigationSchedule> findByZoneIdIn(Collection<Long> zoneIds, Pageable pageable);

    /** Scheduled or running irrigations of a zone starting within [start, end[. */
    default List<IrrigationSchedule> findOpenForZoneBetween(Long zoneId, Instant start, Instant end) {
        return findForZoneBetween(zoneId, IrrigationStatus.OPEN, start, end);
    }

    @Query("select s from IrrigationSchedule s where s.zoneId = :zoneId "
            + "and s.status in :statuses and s.startTime >= :start and s.startTime < :end")
    List<IrrigationSchedule> findForZoneBetween(Long zoneId, Collection<IrrigationStatus> statuses,
                                                Instant start, Instant end);
}
