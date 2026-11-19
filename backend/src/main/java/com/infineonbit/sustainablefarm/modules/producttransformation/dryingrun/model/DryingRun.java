package com.infineonbit.sustainablefarm.modules.producttransformation.dryingrun.model;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.model.Batch;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.model.Operator;

import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent;

import jakarta.persistence.*;
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
@Table(name = "drying_run",
        uniqueConstraints = @UniqueConstraint(name = "uk_drying_run_code", columnNames = "code"),
        indexes = {
        @Index(name = "idx_drying_run_operator_id", columnList = "operator_id"),
        @Index(name = "idx_drying_run_batch_id", columnList = "batch_id"),
        @Index(name = "idx_drying_run_start_time", columnList = "start_time"),
        @Index(name = "idx_drying_run_equipment_id", columnList = "equipment_id")
})
@Getter
@Setter
@ToString
@NoArgsConstructor
public class DryingRun extends BaseEntity {

    /** Business code, assigned by the client and shown by the API; the key is {@code id}. */
    @Column(name = "code", nullable = false, length = 50)
    private String runId;

    // Lazy: the read queries fetch it (entity graph), responses are built in a read-only transaction.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", foreignKey = @ForeignKey(name = "fk_drying_run_batch"), nullable = false)
    @ToString.Exclude
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

    /** The register id of the equipment used (machinery {@code equipment}); the equipment cannot be deleted while used. */
    @Column(name = "equipment_id")
    private Long equipmentId;

    // Lazy: the read queries fetch it (entity graph), responses are built in a read-only transaction.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id", foreignKey = @ForeignKey(name = "fk_drying_run_operator"))
    @ToString.Exclude
    private Operator operator;

    /**
     * Validate moisture content against EU requirements
     * Business Rule: Target moisture 12-18% (range: 6-17.44%)
     */
    @PrePersist
    @PreUpdate
    void validateMoistureContent() {
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