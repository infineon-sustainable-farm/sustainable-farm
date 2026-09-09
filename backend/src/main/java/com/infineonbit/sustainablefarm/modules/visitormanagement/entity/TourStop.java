package com.infineonbit.sustainablefarm.modules.visitormanagement.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A step of the standard guided tour (welcome & briefing, mango orchard,
 * smart drip irrigation, solar plant, processing unit, wrap-up). Content is
 * validated by the module owners and carries the confirmed duration,
 * capacity, possible demo and safety notes.
 */
@Entity
@Table(name = "tour_stop")
public class TourStop extends BaseEntity {

    @NotBlank
    @Size(max = 150)
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @NotNull
    @Min(1)
    @Column(name = "position", nullable = false)
    private int position;

    @Size(max = 2000)
    @Column(name = "description", length = 2000)
    private String description;

    @Min(1)
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Min(1)
    @Column(name = "max_capacity")
    private Integer maxCapacity;

    @Size(max = 150)
    @Column(name = "location", length = 150)
    private String location;

    @Size(max = 1000)
    @Column(name = "demo", length = 1000)
    private String demo;

    @Size(max = 1000)
    @Column(name = "safety_notes", length = 1000)
    private String safetyNotes;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDemo() {
        return demo;
    }

    public void setDemo(String demo) {
        this.demo = demo;
    }

    public String getSafetyNotes() {
        return safetyNotes;
    }

    public void setSafetyNotes(String safetyNotes) {
        this.safetyNotes = safetyNotes;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}