package com.infineonbit.sustainablefarm.modules.visitormanagement.repository;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Event;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.EventStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.EventType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByType(EventType type);

    List<Event> findByStatus(EventStatus status);

    List<Event> findByStartDateTimeBetween(LocalDateTime start, LocalDateTime end);

    List<Event> findByStatusAndEndDateTimeBefore(EventStatus status, LocalDateTime now);

    List<Event> findTop5ByStartDateTimeGreaterThanEqualAndStatusNotOrderByStartDateTimeAsc(
            LocalDateTime from, EventStatus excluded);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") Long id);
}