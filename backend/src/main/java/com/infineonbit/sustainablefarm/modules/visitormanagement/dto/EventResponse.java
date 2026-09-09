package com.infineonbit.sustainablefarm.modules.visitormanagement.dto;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Event;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.EventStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.EventType;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * API representation of an event, including the number of active bookings.
 */
public class EventResponse {

    private Long id;
    private String title;
    private EventType type;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private int maxCapacity;
    private long booked;
    private EventStatus status;
    private String description;
    private String location;
    private Instant createdAt;
    private Instant updatedAt;

    public static EventResponse from(Event event, long bookedCount) {
        EventResponse r = new EventResponse();
        r.id = event.getId();
        r.title = event.getTitle();
        r.type = event.getType();
        r.startDateTime = event.getStartDateTime();
        r.endDateTime = event.getEndDateTime();
        r.maxCapacity = event.getMaxCapacity();
        r.status = event.getStatus();
        r.description = event.getDescription();
        r.location = event.getLocation();
        r.createdAt = event.getCreatedAt();
        r.updatedAt = event.getUpdatedAt();
        r.booked = bookedCount;
        return r;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public EventType getType() {
        return type;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public long getBooked() {
        return booked;
    }

    public EventStatus getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setType(EventType type) { this.type = type; }
    public void setStartDateTime(LocalDateTime startDateTime) { this.startDateTime = startDateTime; }
    public void setEndDateTime(LocalDateTime endDateTime) { this.endDateTime = endDateTime; }
    public void setMaxCapacity(int maxCapacity) { this.maxCapacity = maxCapacity; }
    public void setBooked(long booked) { this.booked = booked; }
    public void setStatus(EventStatus status) { this.status = status; }
    public void setDescription(String description) { this.description = description; }
    public void setLocation(String location) { this.location = location; }
}