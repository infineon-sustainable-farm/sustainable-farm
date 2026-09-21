package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Central holder of the business rules collected from the mockup and
 * questionnaire data, so they can be reused by services and documented.
 */
public final class SchedulingRules {

    private SchedulingRules() {
    }

    /** Maximum visitors per time slot (confirmed with module owners). */
    public static final int MAX_VISITORS_PER_SLOT = 10;

    /** Maximum number of slots per day. */
    public static final int MAX_SLOTS_PER_DAY = 2;

    /** Farm is closed on Sundays. */
    public static final DayOfWeek CLOSED_DAY = DayOfWeek.SUNDAY;

    /** Default morning slot. */
    public static final LocalTime MORNING_START = LocalTime.of(9, 0);
    public static final LocalTime MORNING_END = LocalTime.of(11, 0);

    /** Default afternoon slot. */
    public static final LocalTime AFTERNOON_START = LocalTime.of(14, 0);
    public static final LocalTime AFTERNOON_END = LocalTime.of(16, 0);
}
