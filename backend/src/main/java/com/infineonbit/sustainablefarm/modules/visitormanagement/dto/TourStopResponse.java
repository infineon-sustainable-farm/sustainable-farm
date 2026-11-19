package com.infineonbit.sustainablefarm.modules.visitormanagement.dto;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TourStop;

import java.time.Instant;

/**
 * API representation of a guided-tour stop.
 */
public class TourStopResponse {

    private Long id;
    private String name;
    private int position;
    private String description;
    private int durationMinutes;
    private Integer maxCapacity;
    private String location;
    private String demo;
    private String safetyNotes;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public static TourStopResponse from(TourStop stop) {
        TourStopResponse r = new TourStopResponse();
        r.id = stop.getId();
        r.name = stop.getName();
        r.position = stop.getPosition();
        r.description = stop.getDescription();
        r.durationMinutes = stop.getDurationMinutes();
        r.maxCapacity = stop.getMaxCapacity();
        r.location = stop.getLocation();
        r.demo = stop.getDemo();
        r.safetyNotes = stop.getSafetyNotes();
        r.active = stop.isActive();
        r.createdAt = stop.getCreatedAt();
        r.updatedAt = stop.getUpdatedAt();
        return r;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPosition() {
        return position;
    }

    public String getDescription() {
        return description;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public String getLocation() {
        return location;
    }

    public String getDemo() {
        return demo;
    }

    public String getSafetyNotes() {
        return safetyNotes;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setPosition(int position) { this.position = position; }
    public void setDescription(String description) { this.description = description; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public void setMaxCapacity(Integer maxCapacity) { this.maxCapacity = maxCapacity; }
    public void setLocation(String location) { this.location = location; }
    public void setDemo(String demo) { this.demo = demo; }
    public void setSafetyNotes(String safetyNotes) { this.safetyNotes = safetyNotes; }
    public void setActive(boolean active) { this.active = active; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}