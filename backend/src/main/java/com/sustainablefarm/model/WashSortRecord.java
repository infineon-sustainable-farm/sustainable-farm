package com.sustainablefarm.model;

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
 * WashSortRecord Entity
 * Washing and sorting stage data - Core Processing Entity
 * 
 * Business Rule: Record created only when batch assigned to washing stage
 * Business Rule: Equipment can be assigned to one batch at a time
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Entity
@Table(name = "wash_sort_record")
@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class WashSortRecord {

    @Id
    @Column(name = "record_id", length = 50)
    private String recordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false, referencedColumnName = "batch_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Batch batch;

    @Column(name = "input_quantity_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal inputQuantityKg;

    @Column(name = "output_quantity_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal outputQuantityKg;

    @Column(name = "waste_quantity_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal wasteQuantityKg;

    @Column(name = "water_usage_liters", nullable = false, precision = 10, scale = 2)
    private BigDecimal waterUsageLiters;

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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new java.sql.Timestamp(System.currentTimeMillis());
    }

    /**
     * Calculate yield percentage
     * Business Rule: Output/Input * 100
     */
    public Double calculateYieldPercentage() {
        if (inputQuantityKg == null || inputQuantityKg.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return outputQuantityKg.divide(inputQuantityKg, 4, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal("100")).doubleValue();
    }

    /**
     * Calculate waste percentage
     * Business Rule: Waste/Input * 100
     */
    public Double calculateWastePercentage() {
        if (inputQuantityKg == null || inputQuantityKg.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return wasteQuantityKg.divide(inputQuantityKg, 4, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal("100")).doubleValue();
    }
}