package com.infineonbit.sustainablefarm.modules.visitormanagement.entity;

/**
 * Lifecycle of an agritourism booking.
 */
public enum BookingStatus {
    /** Awaiting payment confirmation by staff. */
    PENDING,
    /** Payment confirmed, slot reserved. */
    CONFIRMED,
    /** The activity has taken place. */
    COMPLETED,
    /** Cancelled by staff; frees capacity. */
    CANCELLED
}