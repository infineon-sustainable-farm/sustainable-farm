package com.infineonbit.sustainablefarm.modules.visitormanagement.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * A paid agritourism offer available for booking (tour, tasting, workshop...).
 *
 * <p>Business rules: capacity limits how many people can book the activity on a
 * given slot; price in FCFA; payment in cash or mobile money (mockup).</p>
 */
@Entity
@Table(name = "agri_activity")
public class AgriActivity extends BaseEntity {

    @NotBlank(message = "name is required")
    @Size(max = 150, message = "name must be at most 150 characters")
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @NotNull(message = "price is required")
    @DecimalMin(value = "0.0", message = "price must be positive or zero")
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @NotNull(message = "capacity is required")
    @Min(value = 1, message = "capacity must be at least 1")
    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @NotNull(message = "durationMinutes is required")
    @Min(value = 1, message = "durationMinutes must be at least 1")
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Size(max = 1000, message = "description must be at most 1000 characters")
    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}