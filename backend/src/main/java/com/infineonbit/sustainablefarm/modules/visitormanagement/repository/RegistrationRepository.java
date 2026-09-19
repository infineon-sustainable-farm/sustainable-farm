package com.infineonbit.sustainablefarm.modules.visitormanagement.repository;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.BriefingStatus;
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

    @Query("SELECT COUNT(DISTINCT r) FROM Registration r " +
            "WHERE r.timeSlot.date BETWEEN :start AND :end AND r.status NOT IN :excluded")
    long countVisitorsBetween(@Param("start") LocalDate start,
                              @Param("end") LocalDate end,
                              @Param("excluded") Collection<RegistrationStatus> excluded);

    @Query("SELECT COUNT(DISTINCT r.timeSlot.id) FROM Registration r " +
            "WHERE r.timeSlot.date BETWEEN :start AND :end AND r.status NOT IN :excluded")
    long countBusySlotsBetween(@Param("start") LocalDate start,
                               @Param("end") LocalDate end,
                               @Param("excluded") Collection<RegistrationStatus> excluded);

    @Query("SELECT r FROM Registration r " +
            "LEFT JOIN FETCH r.briefing b " +
            "LEFT JOIN FETCH r.visitor v " +
            "LEFT JOIN FETCH r.timeSlot t " +
            "WHERE t.date BETWEEN :start AND :end AND r.status IN :active " +
            "AND (b IS NULL OR b.status <> :done) " +
            "ORDER BY t.date, t.startTime")
    List<Registration> findPendingBriefingsBetween(@Param("start") LocalDate start,
                                                   @Param("end") LocalDate end,
                                                   @Param("active") Collection<RegistrationStatus> active,
                                                   @Param("done") BriefingStatus done);
}
