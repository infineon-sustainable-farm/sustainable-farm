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
     * Volume evite par les irrigations reportees sur un intervalle semi-ouvert [start, end[ :
     * c'est l'economie obtenue en n'arrosant pas quand la pluie annoncee couvrait le besoin.
     */
    @Query("select coalesce(sum(s.waterQuantityLiters), 0) from IrrigationSchedule s "
            + "where lower(s.status) = 'postponed' and s.startTime >= :start and s.startTime < :end")
    double sumPostponedLitersBetween(Instant start, Instant end);
}
