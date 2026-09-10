package com.infineonbit.sustainablefarm.modules.visitormanagement.repository;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Booking;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Data access for agritourism bookings.
 */
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findAllByOrderByIdDesc();

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByActivityId(Long activityId);

    List<Booking> findByTimeSlotDate(LocalDate date);

    List<Booking> findByStatusAndReminderScheduledAtIsNotNullAndReminderSentAtIsNullOrderByReminderScheduledAtAsc(
            BookingStatus status);

    @Query("SELECT COALESCE(SUM(b.peopleCount), 0) FROM Booking b "
            + "WHERE b.activity.id = :activityId AND b.timeSlot.id = :slotId "
            + "AND b.status NOT IN :excluded")
    long sumPeopleCountByActivityAndSlot(@Param("activityId") Long activityId,
                                         @Param("slotId") Long slotId,
                                         @Param("excluded") List<BookingStatus> excluded);

    @Query("SELECT b.activity.id, COALESCE(SUM(b.peopleCount), 0) FROM Booking b "
            + "WHERE b.status NOT IN :excluded GROUP BY b.activity.id")
    List<Object[]> sumPeopleCountGroupedByActivity(@Param("excluded") List<BookingStatus> excluded);

    @Query("SELECT b FROM Booking b WHERE b.status = :status "
            + "AND b.reminderScheduledAt IS NOT NULL AND b.reminderSentAt IS NULL "
            + "AND b.reminderScheduledAt <= :now")
    List<Booking> findDueReminders(@Param("status") BookingStatus status,
                                   @Param("now") Instant now);
}