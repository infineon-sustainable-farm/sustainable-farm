package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "irrigation_schedules", indexes = {
        @Index(name = "idx_irrigation_schedules_zone_id", columnList = "zone_id"),
        @Index(name = "idx_irrigation_schedules_start_time", columnList = "start_time")
})
public class IrrigationSchedule extends BaseEntity {
    @NotNull
    @Column(name = "zone_id", nullable = false)
    private Long zoneId;

    @NotNull
    @Column(nullable = false)
    private Instant startTime;

    @NotNull
    @Column(nullable = false)
    private Integer durationMinutes;

    @NotNull
    @Column(nullable = false)
    private Double waterQuantityLiters;

    @Convert(converter = IrrigationStatus.Converter.class)
    @Column(length = 20)
    private IrrigationStatus status = IrrigationStatus.SCHEDULED;

    @NotNull
    @Column(nullable = false)
    private Long createdBy;

    @Column(name = "postpone_reason", length = 120)
    private String postponeReason;

    /**
     * Origin of the schedule: {@code manual} (entered by a user) or {@code auto}
     * (created by the soil moisture rule, see IrrigationAutomationService).
     */
    @Convert(converter = IrrigationTrigger.Converter.class)
    @ColumnDefault("'manual'")
    @Column(name = "trigger_source", nullable = false, length = 20)
    private IrrigationTrigger triggerSource = IrrigationTrigger.MANUAL;

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
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

    public IrrigationStatus getStatus() {
        return status;
    }

    public void setStatus(IrrigationStatus status) {
        this.status = status;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public String getPostponeReason() {
        return postponeReason;
    }

    public void setPostponeReason(String postponeReason) {
        this.postponeReason = postponeReason;
    }

    public IrrigationTrigger getTriggerSource() {
        return triggerSource;
    }

    public void setTriggerSource(IrrigationTrigger triggerSource) {
        this.triggerSource = triggerSource;
    }
}