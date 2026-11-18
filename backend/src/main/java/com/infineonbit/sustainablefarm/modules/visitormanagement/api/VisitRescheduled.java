package com.infineonbit.sustainablefarm.modules.visitormanagement.api;

import java.time.Instant;

/**
 * A scheduled visit changed: its time slot or event moved, or the booking was edited (time, group
 * name or size). Carries the full new state, so a listener can apply it as an upsert.
 *
 * @param visitId    stable identifier of the visit, as in {@link VisitScheduled}
 * @param groupName  name of the visitor or group, {@code null} for an anonymous registration
 * @param expectedAt new start of the visit (UTC)
 * @param groupSize  number of people expected
 */
public record VisitRescheduled(String visitId, String groupName, Instant expectedAt, int groupSize) {
}
