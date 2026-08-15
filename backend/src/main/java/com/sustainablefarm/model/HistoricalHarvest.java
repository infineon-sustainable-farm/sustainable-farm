package com.sustainablefarm.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * HistoricalHarvest Entity
 * Aggregated historical harvest data for forecasting - Integration Entity
 * 
 * Business Rule: Minimum 7 years required for forecasting
 * Business Rule: Historical data aggregated from harvest events
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Entity
@Table(name = "historical_harvest")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalHarvest {

    @Id
    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "week", nullable = false)
    private Integer week;

    @Enumerated(EnumType.STRING)
    @Column(name = "mango_variety", nullable = false, length = 20)
    private HarvestEvent.MangoVariety mangoVariety;

    @Column(name = "harvest_quantity_kg", nullable = false, precision = 10, scale = 2)
    private Double harvestQuantityKg;

    @Column(name = "quality_grade_a_pct", precision = 5, scale = 2)
    private Double qualityGradeAPct;

    @Column(name = "quality_grade_b_pct", precision = 5, scale = 2)
    private Double qualityGradeBPct;

    @Column(name = "quality_grade_c_pct", precision = 5, scale = 2)
    private Double qualityGradeCPct;

    @Column(name = "weather_condition", length = 50)
    private String weatherCondition;

    @Column(name = "rainfall_mm", precision = 10, scale = 2)
    private Double rainfallMm;

    @Column(name = "temperature_avg_c", precision = 5, scale = 2)
    private Double temperatureAvgC;

    @Column(name = "created_at", updatable = false)
    private java.time.Timestamp createdAt;

    @Column(name = "updated_at")
    private java.time.Timestamp updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new java.sql.Timestamp(System.currentTimeMillis());
        updatedAt = new java.sql.Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new java.sql.Timestamp(System.currentTimeMillis());
    }
}