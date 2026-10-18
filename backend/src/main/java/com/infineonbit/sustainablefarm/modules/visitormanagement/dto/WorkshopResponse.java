package com.infineonbit.sustainablefarm.modules.visitormanagement.dto;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Workshop;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.WorkshopStatus;

import java.time.Instant;

/**
 * API representation of a workshop / tour template.
 */
public class WorkshopResponse {

    private Long id;
    private String name;
    private int durationMinutes;
    private String targetGroup;
    private String facilitator;
    private String description;
    private WorkshopStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public static WorkshopResponse from(Workshop workshop) {
        WorkshopResponse r = new WorkshopResponse();
        r.id = workshop.getId();
        r.name = workshop.getName();
        r.durationMinutes = workshop.getDurationMinutes();
        r.targetGroup = workshop.getTargetGroup();
        r.facilitator = workshop.getFacilitator();
        r.description = workshop.getDescription();
        r.status = workshop.getStatus();
        r.createdAt = workshop.getCreatedAt();
        r.updatedAt = workshop.getUpdatedAt();
        return r;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public String getTargetGroup() {
        return targetGroup;
    }

    public String getFacilitator() {
        return facilitator;
    }

    public String getDescription() {
        return description;
    }

    public WorkshopStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public void setTargetGroup(String targetGroup) { this.targetGroup = targetGroup; }
    public void setFacilitator(String facilitator) { this.facilitator = facilitator; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(WorkshopStatus status) { this.status = status; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}