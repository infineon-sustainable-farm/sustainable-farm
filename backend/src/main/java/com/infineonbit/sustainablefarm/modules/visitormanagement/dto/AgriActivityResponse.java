package com.infineonbit.sustainablefarm.modules.visitormanagement.dto;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.AgriActivity;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * API representation of an agritourism activity.
 */
public class AgriActivityResponse {

    private Long id;
    private String name;
    private BigDecimal price;
    private Integer capacity;
    private Integer durationMinutes;
    private String description;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public static AgriActivityResponse from(AgriActivity activity) {
        AgriActivityResponse r = new AgriActivityResponse();
        r.id = activity.getId();
        r.name = activity.getName();
        r.price = activity.getPrice();
        r.capacity = activity.getCapacity();
        r.durationMinutes = activity.getDurationMinutes();
        r.description = activity.getDescription();
        r.active = activity.isActive();
        r.createdAt = activity.getCreatedAt();
        r.updatedAt = activity.getUpdatedAt();
        return r;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public String getDescription() {
        return description;
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
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public void setDescription(String description) { this.description = description; }
    public void setActive(boolean active) { this.active = active; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}