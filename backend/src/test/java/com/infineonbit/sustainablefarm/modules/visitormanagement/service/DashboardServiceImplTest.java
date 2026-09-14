package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.DashboardResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.EventResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.FeedbackSummaryResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.UpcomingTask;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Booking;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.BookingStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Event;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.EventStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.EventType;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Registration;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlot;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Visitor;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.BookingRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.EventRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.RegistrationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private RegistrationRepository registrationRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventService eventService;
    @Mock
    private FeedbackService feedbackService;
    @InjectMocks
    private DashboardServiceImpl service;

    private Registration registration(String name, LocalDate date) {
        Visitor visitor = new Visitor();
        visitor.setFullName(name);
        TimeSlot slot = new TimeSlot();
        slot.setDate(date);
        slot.setStartTime(LocalTime.of(9, 0));
        Registration r = new Registration();
        r.setVisitor(visitor);
        r.setTimeSlot(slot);
        return r;
    }

    private Booking booking(long id, String name, LocalDate date, Instant reminderAt) {
        TimeSlot slot = new TimeSlot();
        slot.setDate(date);
        slot.setStartTime(LocalTime.of(14, 0));
        Booking b = new Booking();
        b.setId(id);
        b.setTimeSlot(slot);
        b.setVisitorFullName(name);
        b.setPeopleCount(3);
        b.setReminderScheduledAt(reminderAt);
        return b;
    }

    @Test
    void getDashboard_aggregatesKpisAndTasks() {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);

        Registration marie = registration("Marie Dubois", monday);
        Registration diallo = registration("A. Diallo", monday.plusDays(1));
        when(registrationRepository.countVisitorsBetween(eq(monday), eq(sunday), any())).thenReturn(5L);
        when(registrationRepository.countBusySlotsBetween(eq(monday), eq(sunday), any())).thenReturn(3L);
        when(registrationRepository.findPendingBriefingsBetween(eq(monday), eq(sunday), any(), any()))
                .thenReturn(List.of(marie, diallo));
        when(feedbackService.summary(any(Instant.class), any(Instant.class)))
                .thenReturn(FeedbackSummaryResponse.of(4.5, 10, 80.0, Map.of()));

        Event event = new Event();
        event.setId(7L);
        when(eventRepository.findTop5ByStartDateTimeGreaterThanEqualAndStatusNotOrderByStartDateTimeAsc(
                any(LocalDateTime.class), eq(EventStatus.CANCELLED)))
                .thenReturn(List.of(event));
        EventResponse eventResponse = new EventResponse();
        eventResponse.setId(7L);
        eventResponse.setTitle("Open Farm Day");
        eventResponse.setType(EventType.OPEN_DAY);
        when(eventService.getEvent(7L)).thenReturn(eventResponse);

        Booking pending = booking(42L, "Lucas Weber", monday.plusDays(2), null);
        when(bookingRepository.findByStatus(BookingStatus.PENDING)).thenReturn(List.of(pending));
        when(bookingRepository.findByStatusAndReminderScheduledAtIsNotNullAndReminderSentAtIsNullOrderByReminderScheduledAtAsc(
                BookingStatus.CONFIRMED)).thenReturn(Collections.emptyList());

        DashboardResponse dashboard = service.getDashboard();

        assertThat(dashboard.getVisitorsThisWeek()).isEqualTo(5);
        assertThat(dashboard.getSlotsBooked()).isEqualTo(3);
        assertThat(dashboard.getPendingBriefings()).isEqualTo(2);
        assertThat(dashboard.getAverageSatisfaction()).isEqualTo(4.5);
        assertThat(dashboard.getUpcomingEvents()).hasSize(1);
        assertThat(dashboard.getUpcomingEvents().get(0).getTitle()).isEqualTo("Open Farm Day");

        assertThat(dashboard.getUpcomingTasks()).hasSize(3);
        assertThat(dashboard.getUpcomingTasks().get(0).getType()).isEqualTo(UpcomingTask.Type.DELIVER_BRIEFING);
        assertThat(dashboard.getUpcomingTasks().get(0).getLabel()).isEqualTo("Deliver briefing - Marie Dubois");
        assertThat(dashboard.getUpcomingTasks().get(2).getType()).isEqualTo(UpcomingTask.Type.CONFIRM_BOOKING);
        assertThat(dashboard.getUpcomingTasks().get(2).getLabel())
                .isEqualTo("Confirm booking BK-00042 - Lucas Weber");
    }

    @Test
    void getDashboard_weekRangeIsMondayToSunday() {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);
        when(registrationRepository.countVisitorsBetween(eq(monday), eq(sunday), any())).thenReturn(0L);
        when(registrationRepository.countBusySlotsBetween(eq(monday), eq(sunday), any())).thenReturn(0L);
        when(registrationRepository.findPendingBriefingsBetween(eq(monday), eq(sunday), any(), any()))
                .thenReturn(Collections.emptyList());
        when(feedbackService.summary(any(Instant.class), any(Instant.class)))
                .thenReturn(FeedbackSummaryResponse.of(0, 0, 0, Map.of()));
        when(eventRepository.findTop5ByStartDateTimeGreaterThanEqualAndStatusNotOrderByStartDateTimeAsc(
                any(LocalDateTime.class), eq(EventStatus.CANCELLED))).thenReturn(Collections.emptyList());
        when(bookingRepository.findByStatus(BookingStatus.PENDING)).thenReturn(Collections.emptyList());
        when(bookingRepository.findByStatusAndReminderScheduledAtIsNotNullAndReminderSentAtIsNullOrderByReminderScheduledAtAsc(
                BookingStatus.CONFIRMED)).thenReturn(Collections.emptyList());

        service.getDashboard();

        Instant weekFrom = monday.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant weekTo = sunday.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        verify(feedbackService).summary(weekFrom, weekTo);
    }

    @Test
    void getDashboard_reminderTasksAppearSortedByDue() {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);
        when(registrationRepository.countVisitorsBetween(eq(monday), eq(sunday), any())).thenReturn(0L);
        when(registrationRepository.countBusySlotsBetween(eq(monday), eq(sunday), any())).thenReturn(0L);
        when(registrationRepository.findPendingBriefingsBetween(eq(monday), eq(sunday), any(), any()))
                .thenReturn(Collections.emptyList());
        when(feedbackService.summary(any(Instant.class), any(Instant.class)))
                .thenReturn(FeedbackSummaryResponse.of(0, 0, 0, Map.of()));
        when(eventRepository.findTop5ByStartDateTimeGreaterThanEqualAndStatusNotOrderByStartDateTimeAsc(
                any(LocalDateTime.class), eq(EventStatus.CANCELLED))).thenReturn(Collections.emptyList());

        Booking pending = booking(1L, "Marie Dubois", monday.plusDays(2), null);
        Booking reminder = booking(2L, "Lucas Weber", monday.plusDays(3),
                Instant.parse("2026-09-10T12:00:00Z"));
        when(bookingRepository.findByStatus(BookingStatus.PENDING)).thenReturn(List.of(pending));
        when(bookingRepository.findByStatusAndReminderScheduledAtIsNotNullAndReminderSentAtIsNullOrderByReminderScheduledAtAsc(
                BookingStatus.CONFIRMED)).thenReturn(List.of(reminder));

        DashboardResponse dashboard = service.getDashboard();

        assertThat(dashboard.getUpcomingTasks()).hasSize(2);
        assertThat(dashboard.getUpcomingTasks().get(0).getType()).isEqualTo(UpcomingTask.Type.CONFIRM_BOOKING);
        assertThat(dashboard.getUpcomingTasks().get(1).getType()).isEqualTo(UpcomingTask.Type.SEND_REMINDER);
        assertThat(dashboard.getUpcomingTasks().get(1).getLabel()).isEqualTo("Send reminder - Lucas Weber (3)");
    }
}