package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.core.notification.NotificationService;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Booking;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.BookingStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Sends the 24 h pre-visit reminder for confirmed bookings whose scheduled
 * reminder is due and not yet delivered.
 */
@Component
public class BookingReminderJob {

    private static final Logger log = LoggerFactory.getLogger(BookingReminderJob.class);

    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    public BookingReminderJob(BookingRepository bookingRepository,
                              NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(
            fixedDelayString = "${app.mail.reminder-interval-ms:60000}",
            initialDelayString = "${app.mail.reminder-interval-ms:60000}")
    @Transactional
    public void sendDueReminders() {
        for (Booking booking : bookingRepository.findDueReminders(
                BookingStatus.CONFIRMED, Instant.now())) {
            sendReminder(booking);
        }
    }

    private void sendReminder(Booking booking) {
        try {
            BookingEmailBuilder.MailContent mail = BookingEmailBuilder.reminder(booking);
            notificationService.send(booking.getVisitorEmail(), mail.subject(), mail.html());
            booking.setReminderSentAt(Instant.now());
            bookingRepository.save(booking);
        } catch (Exception e) {
            log.warn("Could not send reminder for booking {}: {}",
                    booking.getId(), e.getMessage());
        }
    }
}