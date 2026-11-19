package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Booking;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.BookingStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Event;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.EventStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlot;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlotStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.BookingRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.EventRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.TimeSlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Automatically transitions past visits to COMPLETED so that booking /
 * slot / event lifecycles no longer stay open indefinitely after the date.
 */
@Component
public class VisitLifecycleJob {

    private static final Logger log = LoggerFactory.getLogger(VisitLifecycleJob.class);

    private static final List<BookingStatus> ACTIVE_BOOKINGS =
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    private static final List<TimeSlotStatus> OPEN_SLOT_STATUSES =
            List.of(TimeSlotStatus.AVAILABLE, TimeSlotStatus.RESERVED, TimeSlotStatus.FULL);

    private final BookingRepository bookingRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final EventRepository eventRepository;

    public VisitLifecycleJob(BookingRepository bookingRepository,
                             TimeSlotRepository timeSlotRepository,
                             EventRepository eventRepository) {
        this.bookingRepository = bookingRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.eventRepository = eventRepository;
    }

    @Scheduled(
            fixedDelayString = "${app.lifecycle.interval-ms:300000}",
            initialDelayString = "${app.lifecycle.initial-delay-ms:60000}")
    @Transactional
    public void completePastVisits() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalTime time = now.toLocalTime();

        List<Booking> bookings = bookingRepository.findPastActive(ACTIVE_BOOKINGS, today, time);
        for (Booking booking : bookings) {
            booking.setStatus(BookingStatus.COMPLETED);
        }
        if (!bookings.isEmpty()) {
            bookingRepository.saveAll(bookings);
            log.info("Auto-completed {} past booking(s)", bookings.size());
        }

        List<TimeSlot> slots = timeSlotRepository.findPastOpen(today, time, OPEN_SLOT_STATUSES);
        for (TimeSlot slot : slots) {
            slot.setStatus(TimeSlotStatus.COMPLETED);
        }
        if (!slots.isEmpty()) {
            timeSlotRepository.saveAll(slots);
            log.info("Auto-completed {} past time slot(s)", slots.size());
        }

        List<Event> events = eventRepository.findByStatusAndEndDateTimeBefore(
                EventStatus.PUBLISHED, now);
        for (Event event : events) {
            event.setStatus(EventStatus.COMPLETED);
        }
        if (!events.isEmpty()) {
            eventRepository.saveAll(events);
            log.info("Auto-completed {} past event(s)", events.size());
        }
    }
}