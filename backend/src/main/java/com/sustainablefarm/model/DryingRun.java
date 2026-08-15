package com.sustainablefarm.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;

/**
 * DryingRun Entity
 * Drying process data (core transformation) - Core Processing Entity
 * 
 * Business Rule: Energy availability must be confirmed before drying
 * Business Rule: Target moisture content 12-18% (range: 6-17.44%) for EU compliance
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Entity
@Table(name = "drying_run")
@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DryingRun {

    @Id
    @Column(name = "run_id", length = 50)
    private String runId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false, referencedColumnName = "batch_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Batch batch;

    @Column(name = "duration_hours", nullable = false, precision = 5, scale = 2)
    private Double durationHours;

    @Column(name = "target_temperature_c", nullable = false, precision = 5, scale = 2)
    private Double targetTemperatureC;

    @Column(name = "actual_temperature_c", nullable = false, precision = 5, scale = 2)
    private Double actualTemperatureC;

    @Column(name = "start_moisture_pct", nullable = false, precision = 5, scale = 2)
    private Double startMoisturePct;

    @Column(name = "end_moisture_pct", nullable = false, precision = 5, scale = 2)
    private Double endMoisturePct;

    @Column(name = "energy_usage_kwh", nullable = false, precision = 10, scale = 2)
    private Double energyUsageKwh;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", referencedColumnName = "equipment_id")
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id", referencedColumnName = "operator_id")
    private Operator operator;

    @Column(name = "created_at", updatable = false)
    private java.time.Timestamp createdAt;

    @Column(name = "updated_at")
    private java.time.Timestamp updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new java.sql.Timestamp(System.currentTimeMillis());
        updatedAt = new java.sql.Timestamp(System.currentTimeMillis());
        validateMoistureContent();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new java.sql.Timestamp(System.currentTimeMillis());
        validateMoistureContent();
    }

    /**
     * Validate moisture content against EU requirements
     * Business Rule: Target moisture 12-18% (range: 6-17.44%)
     */
    private void validateMoistureContent() {
        if (endMoisturePct != null && (endMoisturePct < 6 || endMoisturePct > 18)) {
            throw new IllegalArgumentException(
                "End moisture content must be between 6% and 18% for EU compliance. Current: " + endMoisturePct + "%"
            );
        }
    }

    /**
     * Calculate moisture reduction percentage
     * Business Rule: (Start - End) / Start * 100
     */
    public Double calculateMoistureReductionPct() {
        if (startMoisturePct == null || endMoisturePct == null || startMoisturePct == 0) {
            return 0.0;
        }
        return ((startMoisturePct - endMoisturePct) / startMoisturePct) * 100;
    }

    /**
     * Calculate energy efficiency (kWh per kg)
     * Business Rule: Energy / (Batch quantity * yield)
     */
    public Double calculateEnergyEfficiencyKwhPerKg() {
        if (energyUsageKwh == null || batch == null || batch.getHarvestQuantityKg() == null) {
            return 0.0;
        }
        return energyUsageKwh / batch.getHarvestQuantityKg();
    }

    /**
     * Check if drying result is within target range
     * Business Rule: 12-18% is optimal range
     */
    public boolean isWithinTargetRange() {
        return endMoisturePct != null && endMoisturePct >= 12 && endMoisturePct <= 18;
    }
}