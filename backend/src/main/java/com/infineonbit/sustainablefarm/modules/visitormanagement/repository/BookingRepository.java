package com.infineonbit.sustainablefarm.modules.visitormanagement.repository;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Booking;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

/**
 * Data access for agritourism bookings.
 */
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT b FROM Booking b JOIN FETCH b.activity JOIN FETCH b.timeSlot "
            + "ORDER BY b.id DESC")
    List<Booking> findAllByOrderByIdDesc();

    @Query("SELECT b FROM Booking b JOIN FETCH b.activity JOIN FETCH b.timeSlot "
            + "WHERE b.status = :status")
    List<Booking> findByStatus(@Param("status") BookingStatus status);

    @Query("SELECT b FROM Booking b JOIN FETCH b.activity JOIN FETCH b.timeSlot "
            + "WHERE b.activity.id = :activityId")
    List<Booking> findByActivityId(@Param("activityId") Long activityId);

    @Query("SELECT b FROM Booking b JOIN FETCH b.activity JOIN FETCH b.timeSlot "
            + "WHERE b.timeSlot.date = :date")
    List<Booking> findByTimeSlotDate(@Param("date") LocalDate date);

    List<Booking> findByStatusAndReminderScheduledAtIsNotNullAndReminderSentAtIsNullOrderByReminderScheduledAtAsc(
            BookingStatus status);

    @Query("SELECT COALESCE(SUM(b.peopleCount), 0) FROM Booking b "
            + "WHERE b.activity.id = :activityId AND b.timeSlot.id = :slotId "
            + "AND b.status NOT IN :excluded")
    long sumPeopleCountByActivityAndSlot(@Param("activityId") Long activityId,
                                         @Param("slotId") Long slotId,
                                         @Param("excluded") List<BookingStatus> excluded);

    @Query("SELECT COALESCE(SUM(b.peopleCount), 0) FROM Booking b "
            + "WHERE b.timeSlot.id = :slotId AND b.status NOT IN :excluded")
    long sumPeopleCountBySlot(@Param("slotId") Long slotId,
                              @Param("excluded") List<BookingStatus> excluded);

    @Query("SELECT b.activity.id, COALESCE(SUM(b.peopleCount), 0) FROM Booking b "
            + "WHERE b.status NOT IN :excluded GROUP BY b.activity.id")
    List<Object[]> sumPeopleCountGroupedByActivity(@Param("excluded") List<BookingStatus> excluded);

    @Query("SELECT b FROM Booking b WHERE b.status = :status "
            + "AND b.reminderScheduledAt IS NOT NULL AND b.reminderSentAt IS NULL "
            + "AND b.reminderScheduledAt <= :now")
    List<Booking> findDueReminders(@Param("status") BookingStatus status,
                                   @Param("now") Instant now);

    List<Booking> findByTimeSlotIdAndStatusIn(Long timeSlotId, Collection<BookingStatus> statuses);

    List<Booking> findByActivityIdAndStatusIn(Long activityId, Collection<BookingStatus> statuses);

    @Query("SELECT b FROM Booking b JOIN FETCH b.timeSlot t "
            + "WHERE b.status IN :active "
            + "AND (t.date < :today OR (t.date = :today AND t.endTime <= :now))")
    List<Booking> findPastActive(@Param("active") Collection<BookingStatus> active,
                                 @Param("today") LocalDate today,
                                 @Param("now") LocalTime now);
}