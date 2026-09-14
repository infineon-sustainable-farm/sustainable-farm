package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.notification.NotificationService;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.AgriActivityRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.AgriActivityResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.BookingOccupancyResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.BookingRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.BookingResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.AgriActivity;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Booking;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.BookingPaymentStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.BookingStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlot;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlotStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.AgriActivityRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.BookingRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.TimeSlotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private AgriActivityRepository activityRepository;
    @Mock
    private TimeSlotRepository timeSlotRepository;
    @Mock
    private NotificationService notificationService;
    @InjectMocks
    private BookingServiceImpl service;

    private Long activityId = 1L;
    private Long slotId = 2L;
    private Long bookingId = 3L;

    private AgriActivity buildActivity(int capacity) {
        AgriActivity a = new AgriActivity();
        a.setId(activityId);
        a.setName("Solar workshop");
        a.setPrice(new BigDecimal("5000"));
        a.setCapacity(capacity);
        a.setDurationMinutes(45);
        a.setActive(true);
        return a;
    }

    private TimeSlot buildSlot() {
        TimeSlot s = new TimeSlot();
        s.setId(slotId);
        s.setDate(LocalDate.of(2026, 8, 26));
        s.setStartTime(LocalTime.of(14, 0));
        s.setEndTime(LocalTime.of(16, 0));
        s.setMaxCapacity(10);
        s.setStatus(TimeSlotStatus.AVAILABLE);
        return s;
    }

    private Booking buildBooking(BookingStatus status, BookingPaymentStatus payment,
                                 int peopleCount) {
        Booking b = new Booking();
        b.setId(bookingId);
        b.setActivity(buildActivity(10));
        b.setTimeSlot(buildSlot());
        b.setVisitorFullName("Lucas Weber");
        b.setVisitorEmail("lucas@example.com");
        b.setPeopleCount(peopleCount);
        b.setStatus(status);
        b.setPaymentStatus(payment);
        return b;
    }

    private AgriActivityRequest buildActivityRequest() {
        AgriActivityRequest req = new AgriActivityRequest();
        req.setName("Solar workshop");
        req.setPrice(new BigDecimal("5000"));
        req.setCapacity(15);
        req.setDurationMinutes(45);
        return req;
    }

    private BookingRequest buildBookingRequest(int peopleCount) {
        BookingRequest req = new BookingRequest();
        req.setActivityId(activityId);
        req.setTimeSlotId(slotId);
        req.setVisitorFullName("Lucas Weber");
        req.setVisitorEmail("lucas@example.com");
        req.setVisitorPhone("+22612345678");
        req.setPeopleCount(peopleCount);
        return req;
    }

    @Test
    void createActivity_success() {
        when(activityRepository.save(any(AgriActivity.class))).thenAnswer(inv -> inv.getArgument(0));

        AgriActivityResponse response = service.createActivity(buildActivityRequest());

        assertThat(response.getName()).isEqualTo("Solar workshop");
        assertThat(response.getCapacity()).isEqualTo(15);
        assertThat(response.isActive()).isTrue();
    }

    @Test
    void updateActivity_success() {
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(buildActivity(10)));
        when(activityRepository.save(any(AgriActivity.class))).thenAnswer(inv -> inv.getArgument(0));

        AgriActivityResponse response = service.updateActivity(activityId, buildActivityRequest());

        assertThat(response.getCapacity()).isEqualTo(15);
    }

    @Test
    void deactivateActivity_success() {
        AgriActivity activity = buildActivity(10);
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(activityRepository.save(any(AgriActivity.class))).thenAnswer(inv -> inv.getArgument(0));

        service.deactivateActivity(activityId);

        assertThat(activity.isActive()).isFalse();
    }

    @Test
    void createBooking_success() {
        AgriActivity activity = buildActivity(10);
        TimeSlot slot = buildSlot();
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(timeSlotRepository.findById(slotId)).thenReturn(Optional.of(slot));
        when(bookingRepository.sumPeopleCountByActivityAndSlot(eq(activityId), eq(slotId), anyList()))
                .thenReturn(0L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(bookingId);
            return b;
        });

        BookingResponse response = service.createBooking(buildBookingRequest(3));

        assertThat(response.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(response.getPaymentStatus()).isEqualTo(BookingPaymentStatus.UNPAID);
        assertThat(response.getReference()).isEqualTo("BK-00003");
    }

    @Test
    void createBooking_exceedsSlotCapacity_throws() {
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(buildActivity(10)));
        when(timeSlotRepository.findById(slotId)).thenReturn(Optional.of(buildSlot()));
        when(bookingRepository.sumPeopleCountByActivityAndSlot(eq(activityId), eq(slotId), anyList()))
                .thenReturn(0L);

        assertThatThrownBy(() -> service.createBooking(buildBookingRequest(15)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("slot capacity");
    }

    @Test
    void createBooking_exceedsActivityCapacity_throws() {
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(buildActivity(10)));
        when(timeSlotRepository.findById(slotId)).thenReturn(Optional.of(buildSlot()));
        when(bookingRepository.sumPeopleCountByActivityAndSlot(eq(activityId), eq(slotId), anyList()))
                .thenReturn(8L);

        assertThatThrownBy(() -> service.createBooking(buildBookingRequest(3)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("remaining capacity");
    }

    @Test
    void updateBooking_success_recomputesCapacity() {
        Booking booking = buildBooking(BookingStatus.PENDING, BookingPaymentStatus.UNPAID, 2);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(buildActivity(10)));
        when(timeSlotRepository.findById(slotId)).thenReturn(Optional.of(buildSlot()));
        when(bookingRepository.sumPeopleCountByActivityAndSlot(eq(activityId), eq(slotId), anyList()))
                .thenReturn(5L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse response = service.updateBooking(bookingId, buildBookingRequest(4));

        assertThat(response.getPeopleCount()).isEqualTo(4);
    }

    @Test
    void updateBooking_cancelled_throws() {
        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(buildBooking(BookingStatus.CANCELLED,
                        BookingPaymentStatus.UNPAID, 2)));

        assertThatThrownBy(() -> service.updateBooking(bookingId, buildBookingRequest(3)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cancelled");
    }

    @Test
    void markPaid_success() {
        Booking booking = buildBooking(BookingStatus.PENDING, BookingPaymentStatus.UNPAID, 3);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse response = service.markPaid(bookingId);

        assertThat(response.getPaymentStatus()).isEqualTo(BookingPaymentStatus.PAID);
    }

    @Test
    void markPaid_cancelled_throws() {
        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(buildBooking(BookingStatus.CANCELLED,
                        BookingPaymentStatus.UNPAID, 3)));

        assertThatThrownBy(() -> service.markPaid(bookingId))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void confirmBooking_success_schedulesReminder() {
        Booking booking = buildBooking(BookingStatus.PENDING, BookingPaymentStatus.PAID, 3);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse response = service.confirmBooking(bookingId);

        assertThat(response.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        Instant expected = LocalDate.of(2026, 8, 26).atTime(14, 0)
                .minusHours(24).toInstant(ZoneOffset.UTC);
        assertThat(booking.getReminderScheduledAt()).isEqualTo(expected);
        assertThat(booking.getConfirmationSentAt()).isNotNull();
        verify(notificationService).send(eq(booking.getVisitorEmail()), anyString(), anyString());
    }

    @Test
    void confirmBooking_emailFailure_confirmsAnyway() {
        Booking booking = buildBooking(BookingStatus.PENDING, BookingPaymentStatus.PAID, 3);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("relay down"))
                .when(notificationService).send(anyString(), anyString(), anyString());

        BookingResponse response = service.confirmBooking(bookingId);

        assertThat(response.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getConfirmationSentAt()).isNull();
    }

    @Test
    void confirmBooking_unpaid_throws() {
        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(buildBooking(BookingStatus.PENDING,
                        BookingPaymentStatus.UNPAID, 3)));

        assertThatThrownBy(() -> service.confirmBooking(bookingId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("paid");
    }

    @Test
    void completeBooking_success() {
        Booking booking = buildBooking(BookingStatus.CONFIRMED, BookingPaymentStatus.PAID, 3);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse response = service.completeBooking(bookingId);

        assertThat(response.getStatus()).isEqualTo(BookingStatus.COMPLETED);
    }

    @Test
    void cancelBooking_paidBooking_refunds() {
        Booking booking = buildBooking(BookingStatus.CONFIRMED, BookingPaymentStatus.PAID, 3);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse response = service.cancelBooking(bookingId);

        assertThat(response.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(response.getPaymentStatus()).isEqualTo(BookingPaymentStatus.REFUNDED);
    }

    @Test
    void cancelBooking_completed_throws() {
        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(buildBooking(BookingStatus.COMPLETED,
                        BookingPaymentStatus.PAID, 3)));

        assertThatThrownBy(() -> service.cancelBooking(bookingId))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void listBookings_byStatus() {
        Booking booking = buildBooking(BookingStatus.PENDING, BookingPaymentStatus.UNPAID, 3);
        when(bookingRepository.findByStatus(BookingStatus.PENDING))
                .thenReturn(List.of(booking));

        List<BookingResponse> responses = service.listBookings(BookingStatus.PENDING, null, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void occupancy_computesPercentages() {
        AgriActivity activity = buildActivity(10);
        when(activityRepository.findAll()).thenReturn(List.of(activity));
        when(bookingRepository.sumPeopleCountGroupedByActivity(anyList()))
                .thenReturn(List.<Object[]>of(new Object[]{activityId, 5L}));

        List<BookingOccupancyResponse> responses = service.getOccupancy();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getBooked()).isEqualTo(5);
        assertThat(responses.get(0).getOccupancyPercent()).isEqualTo(50);
    }
}