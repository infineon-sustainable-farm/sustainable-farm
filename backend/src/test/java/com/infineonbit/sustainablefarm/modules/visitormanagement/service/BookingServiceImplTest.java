package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.money.CurrencyCode;
import com.infineonbit.sustainablefarm.core.money.Money;
import com.infineonbit.sustainablefarm.modules.visitormanagement.api.VisitCancelled;
import com.infineonbit.sustainablefarm.modules.visitormanagement.api.VisitRescheduled;
import com.infineonbit.sustainablefarm.modules.visitormanagement.api.VisitScheduled;
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
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Visitor;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.AgriActivityRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.BookingRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.TimeSlotRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.VisitorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
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
    private VisitorRepository visitorRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @InjectMocks
    private BookingServiceImpl service;

    @BeforeEach
    void visitorsAreSavedAsGiven() {
        lenient().when(visitorRepository.save(any(Visitor.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Long activityId = 1L;
    private Long slotId = 2L;
    private Long bookingId = 3L;

    private AgriActivity buildActivity(int capacity) {
        AgriActivity a = new AgriActivity();
        a.setId(activityId);
        a.setName("Solar workshop");
        a.setPrice(Money.of("5000", CurrencyCode.XOF));
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
        b.setVisitor(visitor("Lucas Weber", "lucas@example.com"));
        b.setPeopleCount(peopleCount);
        b.setStatus(status);
        b.setPaymentStatus(payment);
        return b;
    }

    private static Visitor visitor(String fullName, String email) {
        Visitor v = new Visitor();
        v.setFullName(fullName);
        v.setEmail(email);
        return v;
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
        when(activityRepository.findByIdForUpdate(activityId)).thenReturn(Optional.of(activity));
        when(timeSlotRepository.findByIdForUpdate(slotId)).thenReturn(Optional.of(slot));
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
        when(activityRepository.findByIdForUpdate(activityId)).thenReturn(Optional.of(buildActivity(10)));
        when(timeSlotRepository.findByIdForUpdate(slotId)).thenReturn(Optional.of(buildSlot()));
        when(bookingRepository.sumPeopleCountByActivityAndSlot(eq(activityId), eq(slotId), anyList()))
                .thenReturn(0L);

        assertThatThrownBy(() -> service.createBooking(buildBookingRequest(15)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("slot capacity");
    }

    @Test
    void createBooking_exceedsActivityCapacity_throws() {
        when(activityRepository.findByIdForUpdate(activityId)).thenReturn(Optional.of(buildActivity(10)));
        when(timeSlotRepository.findByIdForUpdate(slotId)).thenReturn(Optional.of(buildSlot()));
        when(bookingRepository.sumPeopleCountByActivityAndSlot(eq(activityId), eq(slotId), anyList()))
                .thenReturn(8L);

        assertThatThrownBy(() -> service.createBooking(buildBookingRequest(3)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("remaining capacity");
    }

    @Test
    void createBooking_exceedsSlotCumulativeCapacity_throws() {
        when(activityRepository.findByIdForUpdate(activityId)).thenReturn(Optional.of(buildActivity(20)));
        when(timeSlotRepository.findByIdForUpdate(slotId)).thenReturn(Optional.of(buildSlot()));
        when(bookingRepository.sumPeopleCountByActivityAndSlot(eq(activityId), eq(slotId), anyList()))
                .thenReturn(0L);
        when(bookingRepository.sumPeopleCountBySlot(eq(slotId), anyList())).thenReturn(9L);

        assertThatThrownBy(() -> service.createBooking(buildBookingRequest(3)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("slot");
    }

    @Test
    void updateBooking_success_recomputesCapacity() {
        Booking booking = buildBooking(BookingStatus.PENDING, BookingPaymentStatus.UNPAID, 2);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(activityRepository.findByIdForUpdate(activityId)).thenReturn(Optional.of(buildActivity(10)));
        when(timeSlotRepository.findByIdForUpdate(slotId)).thenReturn(Optional.of(buildSlot()));
        when(bookingRepository.sumPeopleCountByActivityAndSlot(eq(activityId), eq(slotId), anyList()))
                .thenReturn(5L);
        when(bookingRepository.sumPeopleCountBySlot(eq(slotId), anyList())).thenReturn(5L);
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
    void confirmBooking_success_schedulesReminderAndPublishesEvent() {
        Booking booking = buildBooking(BookingStatus.PENDING, BookingPaymentStatus.PAID, 3);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse response = service.confirmBooking(bookingId);

        assertThat(response.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        Instant expected = LocalDate.of(2026, 8, 26).atTime(14, 0)
                .minusHours(24).toInstant(ZoneOffset.UTC);
        assertThat(booking.getReminderScheduledAt()).isEqualTo(expected);
        assertThat(booking.getConfirmationSentAt()).isNull();

        ArgumentCaptor<BookingNotificationEvent> captor =
                ArgumentCaptor.forClass(BookingNotificationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().bookingId()).isEqualTo(bookingId);
        assertThat(captor.getValue().type())
                .isEqualTo(BookingNotificationEvent.Type.CONFIRMATION);
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

    @Test
    void occupancy_capsAt100WhenOverbooked() {
        AgriActivity activity = buildActivity(10);
        when(activityRepository.findAll()).thenReturn(List.of(activity));
        when(bookingRepository.sumPeopleCountGroupedByActivity(anyList()))
                .thenReturn(List.<Object[]>of(new Object[]{activityId, 25L}));

        List<BookingOccupancyResponse> responses = service.getOccupancy();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getOccupancyPercent()).isEqualTo(100);
    }

    // --- Visitor link ---

    private void stubFreeSlot() {
        when(activityRepository.findByIdForUpdate(activityId)).thenReturn(Optional.of(buildActivity(10)));
        when(timeSlotRepository.findByIdForUpdate(slotId)).thenReturn(Optional.of(buildSlot()));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(bookingId);
            return b;
        });
    }

    @Test
    void createBooking_newEmail_createsTheVisitorAndKeepsTheJsonFields() {
        stubFreeSlot();
        when(visitorRepository.findFirstByEmailIgnoreCaseOrderByIdAsc("lucas@example.com"))
                .thenReturn(Optional.empty());

        BookingResponse response = service.createBooking(buildBookingRequest(3));

        ArgumentCaptor<Visitor> saved = ArgumentCaptor.forClass(Visitor.class);
        verify(visitorRepository).save(saved.capture());
        assertThat(saved.getValue().getId()).isNull();
        assertThat(saved.getValue().getFullName()).isEqualTo("Lucas Weber");
        assertThat(saved.getValue().getEmail()).isEqualTo("lucas@example.com");
        assertThat(saved.getValue().getPhone()).isEqualTo("+22612345678");
        assertThat(saved.getValue().getGroupSize()).isEqualTo(3);
        assertThat(response.getVisitorFullName()).isEqualTo("Lucas Weber");
        assertThat(response.getVisitorEmail()).isEqualTo("lucas@example.com");
        assertThat(response.getVisitorPhone()).isEqualTo("+22612345678");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("15000");
        assertThat(response.getActivityPrice()).isEqualByComparingTo("5000");
    }

    @Test
    void createBooking_knownEmail_linksTheExistingVisitor() {
        stubFreeSlot();
        Visitor known = visitor("Lucas W.", "Lucas@Example.com");
        known.setId(42L);
        known.setPhone("+22600000000");
        when(visitorRepository.findFirstByEmailIgnoreCaseOrderByIdAsc("lucas@example.com"))
                .thenReturn(Optional.of(known));
        ArgumentCaptor<Booking> booking = ArgumentCaptor.forClass(Booking.class);

        BookingResponse response = service.createBooking(buildBookingRequest(3));

        verify(bookingRepository).save(booking.capture());
        assertThat(booking.getValue().getVisitor()).isSameAs(known);
        assertThat(known.getFullName()).isEqualTo("Lucas Weber");
        assertThat(known.getPhone()).isEqualTo("+22612345678");
        assertThat(response.getVisitorFullName()).isEqualTo("Lucas Weber");
    }

    @Test
    void createBooking_withoutEmail_findsTheVisitorByName() {
        stubFreeSlot();
        Visitor known = visitor("Lucas Weber", null);
        known.setId(42L);
        when(visitorRepository.findFirstByFullNameIgnoreCaseAndEmailIsNullOrderByIdAsc("Lucas Weber"))
                .thenReturn(Optional.of(known));
        BookingRequest request = buildBookingRequest(2);
        request.setVisitorEmail(" ");
        ArgumentCaptor<Booking> booking = ArgumentCaptor.forClass(Booking.class);

        service.createBooking(request);

        verify(bookingRepository).save(booking.capture());
        assertThat(booking.getValue().getVisitor()).isSameAs(known);
        verify(visitorRepository, never()).findFirstByEmailIgnoreCaseOrderByIdAsc(any());
    }

    @Test
    void createBooking_storesTheTotalInFcfa() {
        stubFreeSlot();
        ArgumentCaptor<Booking> booking = ArgumentCaptor.forClass(Booking.class);

        service.createBooking(buildBookingRequest(3));

        verify(bookingRepository).save(booking.capture());
        assertThat(booking.getValue().getTotal()).isEqualTo(Money.of("15000", CurrencyCode.XOF));
    }

    @Test
    void createActivity_storesThePriceInFcfa() {
        ArgumentCaptor<AgriActivity> activity = ArgumentCaptor.forClass(AgriActivity.class);
        when(activityRepository.save(activity.capture())).thenAnswer(inv -> inv.getArgument(0));

        AgriActivityResponse response = service.createActivity(buildActivityRequest());

        assertThat(activity.getValue().getPrice()).isEqualTo(Money.of("5000", CurrencyCode.XOF));
        assertThat(response.getPrice()).isEqualByComparingTo("5000");
    }

    // --- Visit events (consumed by watersupply) ---

    @Test
    void createBooking_publishesVisitScheduled() {
        when(activityRepository.findByIdForUpdate(activityId)).thenReturn(Optional.of(buildActivity(10)));
        when(timeSlotRepository.findByIdForUpdate(slotId)).thenReturn(Optional.of(buildSlot()));
        when(bookingRepository.sumPeopleCountByActivityAndSlot(eq(activityId), eq(slotId), anyList()))
                .thenReturn(0L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(bookingId);
            return b;
        });

        service.createBooking(buildBookingRequest(3));

        verify(eventPublisher).publishEvent(new VisitScheduled("booking-3", "Lucas Weber",
                Instant.parse("2026-08-26T14:00:00Z"), 3));
    }

    @Test
    void updateBooking_movedToAnotherSlot_publishesVisitRescheduled() {
        Booking booking = buildBooking(BookingStatus.PENDING, BookingPaymentStatus.UNPAID, 2);
        TimeSlot other = buildSlot();
        other.setId(7L);
        other.setDate(LocalDate.of(2026, 8, 27));
        other.setStartTime(LocalTime.of(9, 0));
        other.setEndTime(LocalTime.of(11, 0));
        BookingRequest request = buildBookingRequest(4);
        request.setTimeSlotId(7L);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(activityRepository.findByIdForUpdate(activityId)).thenReturn(Optional.of(buildActivity(10)));
        when(timeSlotRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(other));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateBooking(bookingId, request);

        verify(eventPublisher).publishEvent(new VisitRescheduled("booking-3", "Lucas Weber",
                Instant.parse("2026-08-27T09:00:00Z"), 4));
    }

    @Test
    void updateBooking_sameVisit_publishesNothing() {
        Booking booking = buildBooking(BookingStatus.PENDING, BookingPaymentStatus.UNPAID, 2);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(activityRepository.findByIdForUpdate(activityId)).thenReturn(Optional.of(buildActivity(10)));
        when(timeSlotRepository.findByIdForUpdate(slotId)).thenReturn(Optional.of(buildSlot()));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        // Only the phone number changes: the visit itself (time, group, size) stays the same.
        service.updateBooking(bookingId, buildBookingRequest(2));

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void cancelBooking_publishesVisitCancelled() {
        Booking booking = buildBooking(BookingStatus.CONFIRMED, BookingPaymentStatus.PAID, 2);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        service.cancelBooking(bookingId);

        verify(eventPublisher).publishEvent(new VisitCancelled("booking-3"));
    }

    @Test
    void cancelBookingsForSlot_publishesVisitCancelledForEachBooking() {
        Booking booking = buildBooking(BookingStatus.PENDING, BookingPaymentStatus.UNPAID, 2);
        when(bookingRepository.findByTimeSlotIdAndStatusIn(eq(slotId), any())).thenReturn(List.of(booking));

        service.cancelBookingsForSlot(slotId);

        verify(eventPublisher).publishEvent(new VisitCancelled("booking-3"));
    }

    @Test
    void rescheduleBookingsForSlot_publishesTheNewTime() {
        Booking booking = buildBooking(BookingStatus.CONFIRMED, BookingPaymentStatus.PAID, 2);
        booking.getTimeSlot().setStartTime(LocalTime.of(15, 0));
        when(bookingRepository.findByTimeSlotIdAndStatusIn(eq(slotId), any())).thenReturn(List.of(booking));

        service.rescheduleBookingsForSlot(slotId);

        verify(eventPublisher).publishEvent(new VisitRescheduled("booking-3", "Lucas Weber",
                Instant.parse("2026-08-26T15:00:00Z"), 2));
    }

    @Test
    void markPaid_publishesNoVisitEvent() {
        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(buildBooking(BookingStatus.PENDING, BookingPaymentStatus.UNPAID, 2)));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        service.markPaid(bookingId);

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }
}
