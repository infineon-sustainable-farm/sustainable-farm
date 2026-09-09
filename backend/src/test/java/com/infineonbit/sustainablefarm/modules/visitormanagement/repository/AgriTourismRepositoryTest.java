package com.infineonbit.sustainablefarm.modules.visitormanagement.repository;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.AgriActivity;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Booking;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.BookingPaymentMethod;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.BookingStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlot;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlotStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AgriTourismRepositoryTest {

    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private AgriActivityRepository activityRepository;
    @Autowired
    private TimeSlotRepository timeSlotRepository;

    private AgriActivity createActivity(String name, int capacity) {
        AgriActivity a = new AgriActivity();
        a.setName(name);
        a.setPrice(new BigDecimal("5000"));
        a.setCapacity(capacity);
        a.setDurationMinutes(45);
        return activityRepository.save(a);
    }

    private TimeSlot createSlot(LocalDate date) {
        TimeSlot s = new TimeSlot();
        s.setDate(date);
        s.setStartTime(LocalTime.of(9, 0));
        s.setEndTime(LocalTime.of(11, 0));
        s.setMaxCapacity(10);
        s.setStatus(TimeSlotStatus.AVAILABLE);
        return timeSlotRepository.save(s);
    }

    private Booking createBooking(AgriActivity activity, TimeSlot slot, int people,
                                  BookingStatus status) {
        Booking b = new Booking();
        b.setActivity(activity);
        b.setTimeSlot(slot);
        b.setVisitorFullName("Lucas Weber");
        b.setVisitorEmail("lucas@example.com");
        b.setVisitorPhone("+22612345678");
        b.setPeopleCount(people);
        b.setPaymentMethod(BookingPaymentMethod.ORANGE_MONEY);
        b.setStatus(status);
        return bookingRepository.save(b);
    }

    @Test
    void findAllByOrderByIdDesc_returnsNewestFirst() {
        Booking first = createBooking(createActivity("Tour", 10),
                createSlot(LocalDate.of(2026, 8, 20)), 3, BookingStatus.CONFIRMED);
        Booking second = createBooking(createActivity("Tasting", 15),
                createSlot(LocalDate.of(2026, 8, 26)), 5, BookingStatus.PENDING);

        List<Booking> result = bookingRepository.findAllByOrderByIdDesc();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(second.getId());
        assertThat(result.get(1).getId()).isEqualTo(first.getId());
    }

    @Test
    void findByStatus_filters() {
        createBooking(createActivity("Tour", 10),
                createSlot(LocalDate.of(2026, 8, 20)), 3, BookingStatus.CONFIRMED);
        createBooking(createActivity("Tasting", 15),
                createSlot(LocalDate.of(2026, 8, 26)), 5, BookingStatus.PENDING);

        List<Booking> result = bookingRepository.findByStatus(BookingStatus.PENDING);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getVisitorFullName()).isEqualTo("Lucas Weber");
    }

    @Test
    void findByActivityId_filters() {
        AgriActivity tour = createActivity("Tour", 10);
        createBooking(tour, createSlot(LocalDate.of(2026, 8, 20)), 3, BookingStatus.CONFIRMED);
        createBooking(createActivity("Tasting", 15),
                createSlot(LocalDate.of(2026, 8, 26)), 5, BookingStatus.PENDING);

        List<Booking> result = bookingRepository.findByActivityId(tour.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActivity().getName()).isEqualTo("Tour");
    }

    @Test
    void findByTimeSlotDate_filters() {
        createBooking(createActivity("Tour", 10),
                createSlot(LocalDate.of(2026, 8, 20)), 3, BookingStatus.CONFIRMED);
        createBooking(createActivity("Tasting", 15),
                createSlot(LocalDate.of(2026, 8, 26)), 5, BookingStatus.PENDING);

        List<Booking> result = bookingRepository.findByTimeSlotDate(LocalDate.of(2026, 8, 26));

        assertThat(result).hasSize(1);
    }

    @Test
    void sumPeopleCountByActivityAndSlot_excludesCancelled() {
        AgriActivity tour = createActivity("Tour", 10);
        TimeSlot slot = createSlot(LocalDate.of(2026, 8, 20));
        createBooking(tour, slot, 3, BookingStatus.CONFIRMED);
        createBooking(tour, slot, 2, BookingStatus.CANCELLED);
        AgriActivity other = createActivity("Tasting", 15);
        createBooking(other, createSlot(LocalDate.of(2026, 8, 21)), 7, BookingStatus.CONFIRMED);

        long sum = bookingRepository.sumPeopleCountByActivityAndSlot(
                tour.getId(), slot.getId(), List.of(BookingStatus.CANCELLED));

        assertThat(sum).isEqualTo(3);
    }

    @Test
    void sumPeopleCountGroupedByActivity_groupsByActivity() {
        AgriActivity tour = createActivity("Tour", 10);
        createBooking(tour, createSlot(LocalDate.of(2026, 8, 20)), 3, BookingStatus.CONFIRMED);
        createBooking(tour, createSlot(LocalDate.of(2026, 8, 21)), 2, BookingStatus.PENDING);
        createBooking(tour, createSlot(LocalDate.of(2026, 8, 22)), 4, BookingStatus.CANCELLED);
        createBooking(createActivity("Tasting", 15),
                createSlot(LocalDate.of(2026, 8, 26)), 7, BookingStatus.CONFIRMED);
        AgriActivity tasting = activityRepository.findAllByOrderByNameAsc().stream()
                .filter(a -> a.getName().equals("Tasting"))
                .findFirst().orElseThrow();

        List<Object[]> rows = bookingRepository.sumPeopleCountGroupedByActivity(
                List.of(BookingStatus.CANCELLED));

        assertThat(rows).hasSize(2);
        assertThat(rows).anySatisfy(row ->
                assertBoth(row, tour.getId(), 5));
        assertThat(rows).anySatisfy(row ->
                assertBoth(row, tasting.getId(), 7));
    }

    @Test
    void dueReminders_queryReturnsOnlyDueNotYetSent() {
        AgriActivity tour = createActivity("Tour", 10);
        TimeSlot slot = createSlot(LocalDate.of(2026, 8, 26));
        Instant now = Instant.now();

        Booking due = createBooking(tour, slot, 3, BookingStatus.CONFIRMED);
        due.setReminderScheduledAt(now.minusSeconds(3600));
        bookingRepository.save(due);

        Booking future = createBooking(createActivity("Tasting", 15),
                createSlot(LocalDate.of(2026, 8, 27)), 5, BookingStatus.CONFIRMED);
        future.setReminderScheduledAt(now.plusSeconds(3600));
        bookingRepository.save(future);

        Booking alreadySent = createBooking(createActivity("Sunset", 12),
                createSlot(LocalDate.of(2026, 8, 28)), 2, BookingStatus.CONFIRMED);
        alreadySent.setReminderScheduledAt(now.minusSeconds(7200));
        alreadySent.setReminderSentAt(now);
        bookingRepository.save(alreadySent);

        List<Booking> result = bookingRepository.findDueReminders(BookingStatus.CONFIRMED, now);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(due.getId());
    }

    private void assertBoth(Object[] row, long activityId, long sum) {
        assertThat(((Long) row[0])).isEqualTo(activityId);
        assertThat(((Number) row[1]).longValue()).isEqualTo(sum);
    }
}