package com.infineonbit.sustainablefarm.modules.visitormanagement.entity;

/**
 * Payment state of a booking.
 */
public enum BookingPaymentStatus {
    /** No payment received yet. */
    UNPAID,
    /** Payment received (cash or mobile money). */
    PAID,
    /** Payment returned after cancellation. */
    REFUNDED
}