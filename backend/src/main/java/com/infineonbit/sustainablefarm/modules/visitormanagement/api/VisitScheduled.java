package com.infineonbit.sustainablefarm.modules.visitormanagement.api;

import java.time.Instant;

/**
 * A visit to the farm was scheduled: a booking or a registration was created on a time slot or an
 * event. Published by Visitor Management, delivered to listeners after its transaction commits.
 *
 * @param visitId    stable identifier of the visit, {@code booking-<id>} or {@code registration-<id>}
 * @param groupName  name of the visitor or group, {@code null} for an anonymous registration
 * @param expectedAt start of the visit (UTC)
 * @param groupSize  number of people expected
 */
public record VisitScheduled(String visitId, String groupName, Instant expectedAt, int groupSize) {
}
