package com.infineonbit.sustainablefarm.modules.visitormanagement.dto;

import java.time.Instant;

/**
 * A single dashboard task for the front-desk staff, e.g. "Confirm booking BK-00012",
 * "Send reminder - Lucas Weber (3)" or "Deliver briefing - Marie Dubois".
 */
public class UpcomingTask {

    public enum Type {
        CONFIRM_BOOKING,
        SEND_REMINDER,
        DELIVER_BRIEFING
    }

    private Type type;
    private String label;
    private Instant dueAt;

    public static UpcomingTask of(Type type, String label, Instant dueAt) {
        UpcomingTask t = new UpcomingTask();
        t.type = type;
        t.label = label;
        t.dueAt = dueAt;
        return t;
    }

    public Type getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    public Instant getDueAt() {
        return dueAt;
    }
}