package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.core.notification.NotificationService;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Booking;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

/**
 * Sends booking emails after the surrounding transaction has committed.
 *
 * <p>This keeps confirmation/booking requests fast (A7) while guaranteeing
 * that delivery timestamps are only written when the send really succeeded
 * (I7). Runs on a separate thread and its own transaction so a broken mail
 * relay can never roll back or block the booking.</p>
 */
@Component
public class BookingNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(BookingNotificationListener.class);

    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    public BookingNotificationListener(BookingRepository bookingRepository,
                                       NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.notificationService = notificationService;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingNotification(BookingNotificationEvent event) {
        Booking booking = bookingRepository.findById(event.bookingId()).orElse(null);
        if (booking == null) {
            log.warn("Skipping {} email: booking {} no longer exists",
                    event.type(), event.bookingId());
            return;
        }
        BookingEmailBuilder.MailContent mail = switch (event.type()) {
            case CONFIRMATION -> BookingEmailBuilder.confirmation(booking);
            case REMINDER -> BookingEmailBuilder.reminder(booking);
        };
        if (notificationService.send(booking.getVisitorEmail(), mail.subject(), mail.html())) {
            if (event.type() == BookingNotificationEvent.Type.CONFIRMATION) {
                booking.setConfirmationSentAt(Instant.now());
            } else {
                booking.setReminderSentAt(Instant.now());
            }
            bookingRepository.save(booking);
        }
    }
}