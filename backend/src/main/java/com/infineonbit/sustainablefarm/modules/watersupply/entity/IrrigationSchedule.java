package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "irrigation_schedules")
public class IrrigationSchedule extends BaseEntity {
    @NotNull
    @Column(nullable = false)
    private UUID zoneId;

    @NotNull
    @Column(nullable = false)
    private Instant startTime;

    @NotNull
    @Column(nullable = false)
    private Integer durationMinutes;

    @NotNull
    @Column(nullable = false)
    private Double waterQuantityLiters;

    @Column(length = 20)
    private String status = "scheduled";

    @NotNull
    @Column(nullable = false)
    private UUID createdBy;

    public UUID getZoneId() {
        return zoneId;
    }

    public void setZoneId(UUID zoneId) {
        this.zoneId = zoneId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Double getWaterQuantityLiters() {
        return waterQuantityLiters;
    }

    public void setWaterQuantityLiters(Double waterQuantityLiters) {
        this.waterQuantityLiters = waterQuantityLiters;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }
}