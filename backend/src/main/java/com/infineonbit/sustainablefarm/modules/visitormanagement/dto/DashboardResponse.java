package com.infineonbit.sustainablefarm.modules.visitormanagement.dto;

import java.util.List;

/**
 * Aggregated KPIs for the Visitor Management dashboard: weekly visitor counts,
 * pending safety briefings, average satisfaction, upcoming events and the
 * pending staff tasks (confirm bookings, send reminders, deliver briefings).
 */
public class DashboardResponse {

    private long visitorsThisWeek;
    private long slotsBooked;
    private long pendingBriefings;
    private double averageSatisfaction;
    private List<EventResponse> upcomingEvents;
    private List<UpcomingTask> upcomingTasks;

    public static DashboardResponse of(long visitorsThisWeek, long slotsBooked,
                                       long pendingBriefings, double averageSatisfaction,
                                       List<EventResponse> upcomingEvents,
                                       List<UpcomingTask> upcomingTasks) {
        DashboardResponse r = new DashboardResponse();
        r.visitorsThisWeek = visitorsThisWeek;
        r.slotsBooked = slotsBooked;
        r.pendingBriefings = pendingBriefings;
        r.averageSatisfaction = averageSatisfaction;
        r.upcomingEvents = upcomingEvents;
        r.upcomingTasks = upcomingTasks;
        return r;
    }

    public long getVisitorsThisWeek() {
        return visitorsThisWeek;
    }

    public long getSlotsBooked() {
        return slotsBooked;
    }

    public long getPendingBriefings() {
        return pendingBriefings;
    }

    public double getAverageSatisfaction() {
        return averageSatisfaction;
    }

    public List<EventResponse> getUpcomingEvents() {
        return upcomingEvents;
    }

    public List<UpcomingTask> getUpcomingTasks() {
        return upcomingTasks;
    }
}