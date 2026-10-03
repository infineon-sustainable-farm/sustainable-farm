package com.infineonbit.sustainablefarm.modules.visitormanagement.dto;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlotStatus;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Describes the availability of a single slot for a given date.
 */
public class AvailabilityResponse {

    private Long id;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private int maxCapacity;
    private int remaining;
    private TimeSlotStatus status;
    private Long guideId;

    public AvailabilityResponse() {
    }

    public AvailabilityResponse(Long id, LocalDate date, LocalTime startTime,
                                LocalTime endTime, int maxCapacity, int remaining,
                                TimeSlotStatus status, Long guideId) {
        this.id = id;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.maxCapacity = maxCapacity;
        this.remaining = remaining;
        this.status = status;
        this.guideId = guideId;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public int getRemaining() {
        return remaining;
    }

    public TimeSlotStatus getStatus() {
        return status;
    }

    public Long getGuideId() {
        return guideId;
    }
}
