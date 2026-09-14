package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.core.notification.NotificationService;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.AgriActivity;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Booking;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.BookingPaymentStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.BookingStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlot;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlotStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingReminderJobTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private NotificationService notificationService;
    @InjectMocks
    private BookingReminderJob job;

    private Booking buildBooking(BookingStatus status, Instant scheduled) {
        AgriActivity activity = new AgriActivity();
        activity.setName("Solar workshop");
        activity.setPrice(new BigDecimal("5000"));

        TimeSlot slot = new TimeSlot();
        slot.setDate(LocalDate.of(2026, 8, 26));
        slot.setStartTime(LocalTime.of(14, 0));
        slot.setEndTime(LocalTime.of(16, 0));

        Booking b = new Booking();
        b.setId(1L);
        b.setActivity(activity);
        b.setTimeSlot(slot);
        b.setVisitorFullName("Lucas Weber");
        b.setVisitorEmail("lucas@example.com");
        b.setPeopleCount(3);
        b.setPaymentStatus(BookingPaymentStatus.PAID);
        b.setStatus(status);
        b.setReminderScheduledAt(scheduled);
        return b;
    }

    @Test
    void sendDueReminders_sendsAndMarks() {
        Booking booking = buildBooking(BookingStatus.CONFIRMED, Instant.now().minusSeconds(3600));
        when(bookingRepository.findDueReminders(eq(BookingStatus.CONFIRMED), any(Instant.class)))
                .thenReturn(List.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        job.sendDueReminders();

        verify(notificationService).send(eq(booking.getVisitorEmail()), anyString(), anyString());
        assertThat(booking.getReminderSentAt()).isNotNull();
        verify(bookingRepository).save(booking);
    }

    @Test
    void sendDueReminders_failureContinuesWithNext() {
        Booking first = buildBooking(BookingStatus.CONFIRMED, Instant.now().minusSeconds(7200));
        Booking second = buildBooking(BookingStatus.CONFIRMED, Instant.now().minusSeconds(3600));
        when(bookingRepository.findDueReminders(eq(BookingStatus.CONFIRMED), any(Instant.class)))
                .thenReturn(List.of(first, second));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("smtp down"))
                .doNothing()
                .when(notificationService).send(anyString(), anyString(), anyString());

        job.sendDueReminders();

        assertThat(first.getReminderSentAt()).isNull();
        assertThat(second.getReminderSentAt()).isNotNull();
        verify(bookingRepository, never()).save(first);
        verify(bookingRepository).save(second);
    }
}