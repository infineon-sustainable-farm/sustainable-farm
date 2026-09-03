package com.infineonbit.sustainablefarm.modules.visitormanagement.repository;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlot;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    List<TimeSlot> findByDate(LocalDate date);

    List<TimeSlot> findByDateAndStatusNot(LocalDate date, TimeSlotStatus excluded);

    List<TimeSlot> findByStatus(TimeSlotStatus status);

    boolean existsByDateAndStartTime(LocalDate date, LocalTime startTime);

    long countByDateAndStatusNot(LocalDate date, TimeSlotStatus excluded);

    @Query("SELECT t FROM TimeSlot t WHERE t.date = :date AND t.status <> :excluded " +
            "ORDER BY t.startTime")
    List<TimeSlot> findActiveByDate(@Param("date") LocalDate date,
                                    @Param("excluded") TimeSlotStatus excluded);
}
