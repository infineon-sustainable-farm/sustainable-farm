package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Entity
@Table(name = "irrigation_logs", indexes = @Index(name = "idx_irrigation_logs_schedule_id", columnList = "schedule_id"))
public class IrrigationLog extends BaseEntity {
    @NotNull
    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    /** Read-only link that declares the foreign key fk_irrigation_logs_schedule; the code writes the schedule_id column. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_irrigation_logs_schedule"))
    private IrrigationSchedule schedule;

    @NotNull
    @Column(nullable = false)
    private Instant actualStartTime;

    private Instant actualEndTime;

    @NotNull
    @Column(nullable = false)
    private Double waterUsedLiters;

    @NotNull
    @Convert(converter = IrrigationLogStatus.Converter.class)
    @Column(nullable = false, length = 20)
    private IrrigationLogStatus status;

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
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

    public IrrigationLogStatus getStatus() {
        return status;
    }

    public void setStatus(IrrigationLogStatus status) {
        this.status = status;
    }
}