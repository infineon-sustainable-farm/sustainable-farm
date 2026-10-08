package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * Soil moisture measurement reported by an IoT sensor.
 * Stored separately to keep the history and feed the control rules.
 */
@Entity
@Table(name = "soil_moisture_readings")
public class SoilMoistureReading extends BaseEntity {

    @NotNull
    @Column(name = "zone_id", nullable = false)
    private UUID zoneId;

    /** Measurement depth (cm). Default value 10 cm when not specified. */
    @Column(name = "depth_cm")
    private Integer depthCm = 10;

    @NotNull
    @Column(name = "moisture_percent", nullable = false)
    private Double moisturePercent;

    @NotNull
    @Column(name = "measured_at", nullable = false)
    private Instant measuredAt;

    public UUID getZoneId() {
        return zoneId;
    }

    public void setZoneId(UUID zoneId) {
        this.zoneId = zoneId;
    }

    public Integer getDepthCm() {
        return depthCm;
    }

    public void setDepthCm(Integer depthCm) {
        this.depthCm = depthCm;
    }

    public Double getMoisturePercent() {
        return moisturePercent;
    }

    public void setMoisturePercent(Double moisturePercent) {
        this.moisturePercent = moisturePercent;
    }

    public Instant getMeasuredAt() {
        return measuredAt;
    }

    public void setMeasuredAt(Instant measuredAt) {
        this.measuredAt = measuredAt;
    }
}
