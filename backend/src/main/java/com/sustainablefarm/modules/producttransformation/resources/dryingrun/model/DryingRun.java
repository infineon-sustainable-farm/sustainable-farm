package com.sustainablefarm.modules.producttransformation.resources.dryingrun.model;
import com.sustainablefarm.modules.producttransformation.resources.batch.model.Batch;
import com.sustainablefarm.modules.producttransformation.resources.equipment.model.Equipment;
import com.sustainablefarm.modules.producttransformation.resources.operator.model.Operator;

import com.sustainablefarm.modules.producttransformation.resources.harvestevent.model.HarvestEvent;

import com.sustainablefarm.modules.producttransformation.resources.equipment.model.Equipment;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;
import java.math.BigDecimal;

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
    private BigDecimal durationHours;

    @Column(name = "target_temperature_c", nullable = false, precision = 5, scale = 2)
    private BigDecimal targetTemperatureC;

    @Column(name = "actual_temperature_c", nullable = false, precision = 5, scale = 2)
    private BigDecimal actualTemperatureC;

    @Column(name = "start_moisture_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal startMoisturePct;

    @Column(name = "end_moisture_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal endMoisturePct;

    @Column(name = "energy_usage_kwh", nullable = false, precision = 10, scale = 2)
    private BigDecimal energyUsageKwh;

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
    private java.sql.Timestamp createdAt;

    @Column(name = "updated_at")
    private java.sql.Timestamp updatedAt;

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
        if (endMoisturePct != null && (endMoisturePct.compareTo(new BigDecimal("6")) < 0 || endMoisturePct.compareTo(new BigDecimal("18")) > 0)) {
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
        if (startMoisturePct == null || endMoisturePct == null || startMoisturePct.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return startMoisturePct.subtract(endMoisturePct).divide(startMoisturePct, 4, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal("100")).doubleValue();
    }

    /**
     * Calculate energy efficiency (kWh per kg)
     * Business Rule: Energy / (Batch quantity * yield)
     */
    public Double calculateEnergyEfficiencyKwhPerKg() {
        if (energyUsageKwh == null || batch == null || batch.getHarvestQuantityKg() == null || batch.getHarvestQuantityKg().compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return energyUsageKwh.divide(batch.getHarvestQuantityKg(), 4, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Check if drying result is within target range
     * Business Rule: 12-18% is optimal range
     */
    public boolean isWithinTargetRange() {
        return endMoisturePct != null && endMoisturePct.compareTo(new BigDecimal("12")) >= 0 && endMoisturePct.compareTo(new BigDecimal("18")) <= 0;
    }
}