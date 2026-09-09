package com.infineonbit.sustainablefarm.modules.visitormanagement.repository;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Registration;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    List<Registration> findByTimeSlotId(Long timeSlotId);

    List<Registration> findByStatus(RegistrationStatus status);

    boolean existsByVisitorId(Long visitorId);

    boolean existsByVisitorIdAndTimeSlotId(Long visitorId, Long timeSlotId);

    @Query("SELECT r FROM Registration r WHERE r.timeSlot.date = :date " +
            "ORDER BY r.timeSlot.startTime, r.visitor.fullName")
    List<Registration> findAllByTimeSlotDate(@Param("date") LocalDate date);

    long countByTimeSlotIdAndStatusNotIn(Long timeSlotId, Collection<RegistrationStatus> statuses);

    long countByTimeSlotIdAndStatus(Long timeSlotId, RegistrationStatus status);

    List<Registration> findByIsProspectTrue();

    List<Registration> findByEventId(Long eventId);

    boolean existsByEventIdAndVisitorId(Long eventId, Long visitorId);

    long countByEventIdAndStatusNotIn(Long eventId, Collection<RegistrationStatus> statuses);
}
