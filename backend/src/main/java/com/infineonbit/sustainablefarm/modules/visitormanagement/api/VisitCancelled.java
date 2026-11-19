package com.infineonbit.sustainablefarm.modules.visitormanagement.api;

/**
 * A scheduled visit will not take place: the booking or registration was cancelled or rejected,
 * directly or through its time slot, activity or event.
 *
 * @param visitId stable identifier of the visit, as in {@link VisitScheduled}
 */
public record VisitCancelled(String visitId) {
}
