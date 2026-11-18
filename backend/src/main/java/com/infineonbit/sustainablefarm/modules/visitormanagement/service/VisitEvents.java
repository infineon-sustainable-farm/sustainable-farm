package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.modules.visitormanagement.api.VisitCancelled;
import com.infineonbit.sustainablefarm.modules.visitormanagement.api.VisitRescheduled;
import com.infineonbit.sustainablefarm.modules.visitormanagement.api.VisitScheduled;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Booking;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Registration;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlot;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Visitor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Builds the visit events of {@code visitormanagement.api} from bookings and registrations.
 *
 * <p>A booking and a registration are both a visit; their ids live in two tables, so the visit id
 * is prefixed ({@code booking-12}, {@code registration-12}). Times are read as UTC, like the
 * booking reminder.</p>
 */
final class VisitEvents {

    private VisitEvents() {
    }

    static String visitId(Booking booking) {
        return "booking-" + booking.getId();
    }

    static String visitId(Registration registration) {
        return "registration-" + registration.getId();
    }

    static VisitScheduled scheduled(Booking booking) {
        return new VisitScheduled(visitId(booking), booking.getVisitorFullName(),
                start(booking.getTimeSlot()), booking.getPeopleCount());
    }

    static VisitRescheduled rescheduled(Booking booking) {
        return new VisitRescheduled(visitId(booking), booking.getVisitorFullName(),
                start(booking.getTimeSlot()), booking.getPeopleCount());
    }

    static VisitCancelled cancelled(Booking booking) {
        return new VisitCancelled(visitId(booking));
    }

    /** {@code start} is the slot or event start the registration is on. */
    static VisitScheduled scheduled(Registration registration, LocalDateTime start) {
        return new VisitScheduled(visitId(registration), groupName(registration),
                start.toInstant(ZoneOffset.UTC), groupSize(registration));
    }

    static VisitRescheduled rescheduled(Registration registration, LocalDateTime start) {
        return new VisitRescheduled(visitId(registration), groupName(registration),
                start.toInstant(ZoneOffset.UTC), groupSize(registration));
    }

    static VisitCancelled cancelled(Registration registration) {
        return new VisitCancelled(visitId(registration));
    }

    static LocalDateTime startOf(TimeSlot slot) {
        return slot.getDate().atTime(slot.getStartTime());
    }

    private static Instant start(TimeSlot slot) {
        return startOf(slot).toInstant(ZoneOffset.UTC);
    }

    private static String groupName(Registration registration) {
        Visitor visitor = registration.getVisitor();
        return visitor == null ? null : visitor.getFullName();
    }

    private static int groupSize(Registration registration) {
        Visitor visitor = registration.getVisitor();
        return visitor == null ? 1 : visitor.getGroupSize();
    }
}
