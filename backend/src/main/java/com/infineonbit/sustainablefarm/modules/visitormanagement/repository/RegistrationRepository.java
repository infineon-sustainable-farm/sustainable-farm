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

    @Query("SELECT r FROM Registration r "
            + "LEFT JOIN FETCH r.visitor LEFT JOIN FETCH r.timeSlot "
            + "WHERE r.timeSlot.id = :timeSlotId")
    List<Registration> findByTimeSlotId(@Param("timeSlotId") Long timeSlotId);

    @Query("SELECT r FROM Registration r "
            + "LEFT JOIN FETCH r.visitor LEFT JOIN FETCH r.timeSlot "
            + "WHERE r.timeSlot.id = :timeSlotId AND r.status NOT IN :statuses")
    List<Registration> findByTimeSlotIdAndStatusNotIn(
            @Param("timeSlotId") Long timeSlotId, @Param("statuses") Collection<RegistrationStatus> statuses);

    @Query("SELECT r FROM Registration r LEFT JOIN FETCH r.visitor LEFT JOIN FETCH r.timeSlot "
            + "WHERE r.eventId = :eventId AND r.status IN :statuses")
    List<Registration> findByEventIdAndStatusIn(
            @Param("eventId") Long eventId, @Param("statuses") Collection<RegistrationStatus> statuses);

    List<Registration> findByStatus(RegistrationStatus status);

    boolean existsByVisitorId(Long visitorId);

    boolean existsByVisitorIdAndTimeSlotId(Long visitorId, Long timeSlotId);

    @Query("SELECT r FROM Registration r LEFT JOIN FETCH r.visitor v LEFT JOIN FETCH r.timeSlot t "
            + "WHERE t.date = :date " +
            "ORDER BY t.startTime, v.fullName")
    List<Registration> findAllByTimeSlotDate(@Param("date") LocalDate date);

    long countByTimeSlotIdAndStatusNotIn(Long timeSlotId, Collection<RegistrationStatus> statuses);

    long countByTimeSlotIdAndStatus(Long timeSlotId, RegistrationStatus status);

    /**
     * Number of people booked on a slot: the sum of the group sizes of the
     * active registrations. A registration without a visitor counts as one
     * person. This is the figure the capacity rules use — counting rows would
     * let a group of ten pass as one visitor.
     */
    @Query("SELECT COALESCE(SUM(COALESCE(v.groupSize, 1)), 0) FROM Registration r "
            + "LEFT JOIN r.visitor v "
            + "WHERE r.timeSlot.id = :timeSlotId AND r.status NOT IN :statuses")
    long sumGroupSizeByTimeSlotIdAndStatusNotIn(
            @Param("timeSlotId") Long timeSlotId, @Param("statuses") Collection<RegistrationStatus> statuses);

    @Query("SELECT r FROM Registration r "
            + "LEFT JOIN FETCH r.visitor LEFT JOIN FETCH r.timeSlot LEFT JOIN FETCH r.briefing "
            + "WHERE r.isProspect = true")
    List<Registration> findByIsProspectTrue();

    @Query("SELECT r FROM Registration r "
            + "LEFT JOIN FETCH r.visitor LEFT JOIN FETCH r.timeSlot LEFT JOIN FETCH r.briefing "
            + "WHERE r.eventId = :eventId")
    List<Registration> findByEventId(@Param("eventId") Long eventId);

    boolean existsByEventIdAndVisitorId(Long eventId, Long visitorId);

    long countByEventIdAndStatusNotIn(Long eventId, Collection<RegistrationStatus> statuses);

    /**
     * Number of people registered on an event: the sum of the group sizes of
     * the active registrations, a registration without a visitor counting as
     * one. Event capacity uses people for the same reason slots do.
     */
    @Query("SELECT COALESCE(SUM(COALESCE(v.groupSize, 1)), 0) FROM Registration r "
            + "LEFT JOIN r.visitor v "
            + "WHERE r.eventId = :eventId AND r.status NOT IN :statuses")
    long sumGroupSizeByEventIdAndStatusNotIn(
            @Param("eventId") Long eventId, @Param("statuses") Collection<RegistrationStatus> statuses);

    @Query("SELECT r.eventId, SUM(COALESCE(v.groupSize, 1)) FROM Registration r "
            + "LEFT JOIN r.visitor v "
            + "WHERE r.eventId IN :eventIds AND r.status NOT IN :excluded GROUP BY r.eventId")
    List<Object[]> sumGroupSizeByEventIds(@Param("eventIds") Collection<Long> eventIds,
                                          @Param("excluded") Collection<RegistrationStatus> excluded);

    @Query("SELECT r.eventId, COUNT(r) FROM Registration r "
            + "WHERE r.eventId IN :eventIds AND r.status NOT IN :excluded GROUP BY r.eventId")
    List<Object[]> countByEventIds(@Param("eventIds") Collection<Long> eventIds,
                                   @Param("excluded") Collection<RegistrationStatus> excluded);

    @Query("SELECT COUNT(DISTINCT r) FROM Registration r " +
            "WHERE r.timeSlot.date BETWEEN :start AND :end AND r.status NOT IN :excluded")
    long countVisitorsBetween(@Param("start") LocalDate start,
                              @Param("end") LocalDate end,
                              @Param("excluded") Collection<RegistrationStatus> excluded);

    @Query("SELECT DISTINCT r.timeSlot.id FROM Registration r " +
            "WHERE r.timeSlot.date BETWEEN :start AND :end AND r.status NOT IN :excluded")
    List<Long> findBusySlotIdsBetween(@Param("start") LocalDate start,
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
