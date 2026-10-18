package com.infineonbit.sustainablefarm.modules.visitormanagement.entity;

/**
 * Lifecycle of a tour time slot (visit calendar).
 */
public enum TimeSlotStatus {
    /** Slot is open and can accept new registrations (mockup: capacity &lt; max). */
    AVAILABLE,
    /** Slot has booked registrations but still has remaining capacity. */
    RESERVED,
    /** Slot has reached its maximum capacity (mockup: 10 visitors). */
    FULL,
    /** Slot has been cancelled by staff. */
    CANCELLED,
    /** The visit has finished. */
    COMPLETED
}
