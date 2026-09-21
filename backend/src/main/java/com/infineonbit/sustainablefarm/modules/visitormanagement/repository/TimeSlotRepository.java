package com.infineonbit.sustainablefarm.modules.visitormanagement.repository;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlot;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlotStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    List<TimeSlot> findByDate(LocalDate date);

    List<TimeSlot> findByDateAndStatusNot(LocalDate date, TimeSlotStatus excluded);

    List<TimeSlot> findByStatus(TimeSlotStatus status);

    long countByDateAndStatusNot(LocalDate date, TimeSlotStatus excluded);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TimeSlot t WHERE t.id = :id")
    Optional<TimeSlot> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN TRUE ELSE FALSE END FROM TimeSlot t "
            + "WHERE t.date = :date AND t.startTime < :endTime AND t.endTime > :startTime")
    boolean existsOverlapping(@Param("date") LocalDate date,
                              @Param("startTime") LocalTime startTime,
                              @Param("endTime") LocalTime endTime);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN TRUE ELSE FALSE END FROM TimeSlot t "
            + "WHERE t.date = :date AND t.startTime < :endTime AND t.endTime > :startTime "
            + "AND t.id <> :excludedId")
    boolean existsOverlappingExcluding(@Param("date") LocalDate date,
                                       @Param("startTime") LocalTime startTime,
                                       @Param("endTime") LocalTime endTime,
                                       @Param("excludedId") Long excludedId);

    @Query("SELECT t FROM TimeSlot t WHERE t.date = :date AND t.status <> :excluded " +
            "ORDER BY t.startTime")
    List<TimeSlot> findActiveByDate(@Param("date") LocalDate date,
                                    @Param("excluded") TimeSlotStatus excluded);

    List<TimeSlot> findByDateGreaterThanEqualOrderByDateAscStartTimeAsc(LocalDate from);

    @Query("SELECT t FROM TimeSlot t "
            + "WHERE (t.date < :today OR (t.date = :today AND t.endTime <= :now)) "
            + "AND t.status IN :statuses")
    List<TimeSlot> findPastOpen(@Param("today") LocalDate today,
                                @Param("now") LocalTime now,
                                @Param("statuses") Collection<TimeSlotStatus> statuses);
}
