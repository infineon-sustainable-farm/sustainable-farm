package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "irrigation_logs")
public class IrrigationLog extends BaseEntity {
    @NotNull
    @Column(nullable = false)
    private UUID scheduleId;

    @NotNull
    @Column(nullable = false)
    private Instant actualStartTime;

    private Instant actualEndTime;

    @NotNull
    @Column(nullable = false)
    private Double waterUsedLiters;

    @NotNull
    @Column(nullable = false, length = 20)
    private String status;

    public UUID getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(UUID scheduleId) {
        this.scheduleId = scheduleId;
    }

    public Instant getActualStartTime() {
        return actualStartTime;
    }

    public void setActualStartTime(Instant actualStartTime) {
        this.actualStartTime = actualStartTime;
    }

    public Instant getActualEndTime() {
        return actualEndTime;
    }

    public void setActualEndTime(Instant actualEndTime) {
        this.actualEndTime = actualEndTime;
    }

    public Double getWaterUsedLiters() {
        return waterUsedLiters;
    }

    public void setWaterUsedLiters(Double waterUsedLiters) {
        this.waterUsedLiters = waterUsedLiters;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}