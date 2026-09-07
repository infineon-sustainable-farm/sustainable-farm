package com.sustainablefarm.modules.producttransformation.resources.historicalharvest.model;
import com.sustainablefarm.modules.producttransformation.resources.harvestevent.model.HarvestEvent;

import com.sustainablefarm.modules.producttransformation.resources.harvestevent.model.HarvestEvent;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.math.BigDecimal;

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
@IdClass(HistoricalHarvestId.class)
@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalHarvest {

    @Id
    @Column(name = "year", nullable = false)
    private Integer year;

    @Id
    @Column(name = "month", nullable = false)
    private Integer month;

    @Id
    @Column(name = "week", nullable = false)
    private Integer week;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "mango_variety", nullable = false, length = 20)
    private HarvestEvent.MangoVariety mangoVariety;

    @Column(name = "harvest_quantity_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal harvestQuantityKg;

    @Column(name = "quality_grade_a_pct", precision = 5, scale = 2)
    private BigDecimal qualityGradeAPct;

    @Column(name = "quality_grade_b_pct", precision = 5, scale = 2)
    private BigDecimal qualityGradeBPct;

    @Column(name = "quality_grade_c_pct", precision = 5, scale = 2)
    private BigDecimal qualityGradeCPct;

    @Column(name = "weather_condition", length = 50)
    private String weatherCondition;

    @Column(name = "rainfall_mm", precision = 10, scale = 2)
    private BigDecimal rainfallMm;

    @Column(name = "temperature_avg_c", precision = 5, scale = 2)
    private BigDecimal temperatureAvgC;

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
}