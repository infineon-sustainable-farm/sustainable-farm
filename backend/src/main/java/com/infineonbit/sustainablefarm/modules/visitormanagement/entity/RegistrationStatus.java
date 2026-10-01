package com.infineonbit.sustainablefarm.modules.visitormanagement.entity;

/**
 * Lifecycle of a visitor registration.
 */
public enum RegistrationStatus {
    /** Awaiting staff approval. */
    PENDING,
    /** Approved; a briefing is generated for this registration. */
    CONFIRMED,
    /** Visitor has checked in on the day of the visit. */
    CHECKED_IN,
    /** Rejected by staff. */
    REJECTED,
    /** Cancelled by the visitor or by staff. */
    CANCELLED
}
