package com.infineonbit.sustainablefarm.modules.visitormanagement.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A reusable tour template / workshop (e.g. standard farm tour, solar
 * workshop, mango tasting, school discovery day) with a target group and a
 * facilitator. Templates follow a DRAFT -> ACTIVE -> INACTIVE lifecycle.
 */
@Entity
@Table(name = "workshop")
public class Workshop extends BaseEntity {

    @NotBlank
    @Size(max = 150)
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @NotNull
    @Min(1)
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @NotBlank
    @Size(max = 60)
    @Column(name = "target_group", nullable = false, length = 60)
    private String targetGroup = "All";

    @Size(max = 100)
    @Column(name = "facilitator", length = 100)
    private String facilitator;

    @Size(max = 1000)
    @Column(name = "description", length = 1000)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WorkshopStatus status = WorkshopStatus.DRAFT;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getTargetGroup() {
        return targetGroup;
    }

    public void setTargetGroup(String targetGroup) {
        this.targetGroup = targetGroup;
    }

    public String getFacilitator() {
        return facilitator;
    }

    public void setFacilitator(String facilitator) {
        this.facilitator = facilitator;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public WorkshopStatus getStatus() {
        return status;
    }

    public void setStatus(WorkshopStatus status) {
        this.status = status;
    }
}