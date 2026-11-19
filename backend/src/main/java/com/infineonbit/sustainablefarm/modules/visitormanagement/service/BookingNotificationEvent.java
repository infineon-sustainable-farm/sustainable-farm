package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

/**
 * Published after a booking transaction commits so the corresponding email
 * can be sent asynchronously (outside the request thread) and its delivery
 * timestamp recorded only once actually delivered.
 */
public record BookingNotificationEvent(Long bookingId, Type type) {

    public enum Type {
        CONFIRMATION,
        REMINDER
    }
}