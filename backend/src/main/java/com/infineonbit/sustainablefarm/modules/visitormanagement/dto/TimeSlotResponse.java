package com.infineonbit.sustainablefarm.modules.visitormanagement.dto;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlot;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlotStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * API representation of a time slot.
 */
public class TimeSlotResponse {

    private Long id;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private int maxCapacity;
    private long booked;
    private TimeSlotStatus status;
    private Long guideId;
    private Instant createdAt;
    private Instant updatedAt;

    public static TimeSlotResponse from(TimeSlot slot, long bookedCount) {
        TimeSlotResponse r = new TimeSlotResponse();
        r.id = slot.getId();
        r.date = slot.getDate();
        r.startTime = slot.getStartTime();
        r.endTime = slot.getEndTime();
        r.maxCapacity = slot.getMaxCapacity();
        r.status = slot.getStatus();
        r.guideId = slot.getGuideId();
        r.createdAt = slot.getCreatedAt();
        r.updatedAt = slot.getUpdatedAt();
        r.booked = bookedCount;
        return r;
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

    public long getBooked() {
        return booked;
    }

    public TimeSlotStatus getStatus() {
        return status;
    }

    public Long getGuideId() {
        return guideId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) { this.id = id; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public void setMaxCapacity(int maxCapacity) { this.maxCapacity = maxCapacity; }
    public void setBooked(long booked) { this.booked = booked; }
    public void setStatus(TimeSlotStatus status) { this.status = status; }
    public void setGuideId(Long guideId) { this.guideId = guideId; }
}
