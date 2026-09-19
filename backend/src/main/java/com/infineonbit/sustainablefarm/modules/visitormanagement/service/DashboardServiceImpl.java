package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.DashboardResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.EventResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.UpcomingTask;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Booking;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.BookingStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.BriefingStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.EventStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Registration;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.RegistrationStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.BookingRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.EventRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.RegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private static final List<RegistrationStatus> EXCLUDED_REGISTRATIONS =
            List.of(RegistrationStatus.REJECTED, RegistrationStatus.CANCELLED);
    private static final List<RegistrationStatus> ACTIVE_REGISTRATIONS =
            List.of(RegistrationStatus.CONFIRMED, RegistrationStatus.CHECKED_IN);

    private final RegistrationRepository registrationRepository;
    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final EventService eventService;
    private final FeedbackService feedbackService;

    public DashboardServiceImpl(RegistrationRepository registrationRepository,
                                BookingRepository bookingRepository,
                                EventRepository eventRepository,
                                EventService eventService,
                                FeedbackService feedbackService) {
        this.registrationRepository = registrationRepository;
        this.bookingRepository = bookingRepository;
        this.eventRepository = eventRepository;
        this.eventService = eventService;
        this.feedbackService = feedbackService;
    }

    @Override
    public DashboardResponse getDashboard() {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);
        Instant weekFrom = monday.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant weekTo = sunday.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        long visitors = registrationRepository.countVisitorsBetween(monday, sunday, EXCLUDED_REGISTRATIONS);
        long slots = registrationRepository.countBusySlotsBetween(monday, sunday, EXCLUDED_REGISTRATIONS);
        List<Registration> pendingBriefings =
                registrationRepository.findPendingBriefingsBetween(monday, sunday, ACTIVE_REGISTRATIONS, BriefingStatus.DONE);
        double satisfaction = feedbackService.summary(weekFrom, weekTo).getAverageRating();

        List<EventResponse> events = eventRepository
                .findTop5ByStartDateTimeGreaterThanEqualAndStatusNotOrderByStartDateTimeAsc(
                        LocalDateTime.now(), EventStatus.CANCELLED)
                .stream()
                .map(event -> eventService.getEvent(event.getId()))
                .toList();

        List<Booking> pending = bookingRepository.findByStatus(BookingStatus.PENDING);
        List<Booking> reminders = bookingRepository
                .findByStatusAndReminderScheduledAtIsNotNullAndReminderSentAtIsNullOrderByReminderScheduledAtAsc(
                        BookingStatus.CONFIRMED);

        List<UpcomingTask> tasks = new ArrayList<>();
        pending.forEach(b -> tasks.add(UpcomingTask.of(UpcomingTask.Type.CONFIRM_BOOKING,
                "Confirm booking " + reference(b) + " - " + b.getVisitorFullName(),
                slotInstant(b))));
        reminders.forEach(b -> tasks.add(UpcomingTask.of(UpcomingTask.Type.SEND_REMINDER,
                "Send reminder - " + b.getVisitorFullName() + " (" + b.getPeopleCount() + ")",
                b.getReminderScheduledAt())));
        pendingBriefings.forEach(r -> tasks.add(UpcomingTask.of(UpcomingTask.Type.DELIVER_BRIEFING,
                "Deliver briefing - " + r.getVisitor().getFullName(),
                slotInstant(r))));
        tasks.sort(Comparator.comparing(UpcomingTask::getDueAt,
                Comparator.nullsLast(Comparator.naturalOrder())));

        return DashboardResponse.of(visitors, slots, pendingBriefings.size(), satisfaction, events, tasks);
    }

    private static String reference(Booking booking) {
        return "BK-" + String.format("%05d", booking.getId() == null ? 0 : booking.getId());
    }

    private static Instant slotInstant(Booking booking) {
        return booking.getTimeSlot().getDate()
                .atTime(booking.getTimeSlot().getStartTime())
                .toInstant(ZoneOffset.UTC);
    }

    private static Instant slotInstant(Registration registration) {
        return registration.getTimeSlot().getDate()
                .atTime(registration.getTimeSlot().getStartTime())
                .toInstant(ZoneOffset.UTC);
    }
}