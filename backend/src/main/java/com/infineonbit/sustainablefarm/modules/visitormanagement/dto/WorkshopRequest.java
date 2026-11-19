package com.infineonbit.sustainablefarm.modules.visitormanagement.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request to create or update a workshop / tour template.
 */
public class WorkshopRequest {

    @NotBlank(message = "name is required")
    @Size(max = 150, message = "name must be at most 150 characters")
    private String name;

    @NotNull(message = "durationMinutes is required")
    @Min(value = 1, message = "durationMinutes must be at least 1")
    private Integer durationMinutes;

    @NotBlank(message = "targetGroup is required")
    @Size(max = 60, message = "targetGroup must be at most 60 characters")
    private String targetGroup;

    /** Person of the people directory who facilitates the workshop, or {@code null}. */
    private Long facilitatorPersonId;

    @Size(max = 1000, message = "description must be at most 1000 characters")
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getTargetGroup() {
        return targetGroup;
    }

    public void setTargetGroup(String targetGroup) {
        this.targetGroup = targetGroup;
    }

    public Long getFacilitatorPersonId() {
        return facilitatorPersonId;
    }

    public void setFacilitatorPersonId(Long facilitatorPersonId) {
        this.facilitatorPersonId = facilitatorPersonId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}