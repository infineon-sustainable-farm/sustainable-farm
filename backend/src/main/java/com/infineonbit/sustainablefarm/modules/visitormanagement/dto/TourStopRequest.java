package com.infineonbit.sustainablefarm.modules.visitormanagement.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request to create or update a guided-tour stop.
 */
public class TourStopRequest {

    @NotBlank(message = "name is required")
    @Size(max = 150, message = "name must be at most 150 characters")
    private String name;

    @NotNull(message = "position is required")
    @Min(value = 1, message = "position must be at least 1")
    private Integer position;

    @Size(max = 2000, message = "description must be at most 2000 characters")
    private String description;

    @NotNull(message = "durationMinutes is required")
    @Min(value = 1, message = "durationMinutes must be at least 1")
    private Integer durationMinutes;

    @Min(value = 1, message = "maxCapacity must be at least 1")
    private Integer maxCapacity;

    @Size(max = 150, message = "location must be at most 150 characters")
    private String location;

    @Size(max = 1000, message = "demo must be at most 1000 characters")
    private String demo;

    @Size(max = 1000, message = "safetyNotes must be at most 1000 characters")
    private String safetyNotes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
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
}