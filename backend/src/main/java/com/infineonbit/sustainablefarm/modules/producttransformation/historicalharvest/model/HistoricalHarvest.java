package com.infineonbit.sustainablefarm.modules.producttransformation.historicalharvest.model;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import com.infineonbit.sustainablefarm.modules.plants.api.VarietyCatalog;

import jakarta.persistence.*;
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
 * Business Rule: one row per year, month, week and variety (uk_historical_harvest_period_variety)
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Entity
@Table(name = "historical_harvest", uniqueConstraints = @UniqueConstraint(
        name = "uk_historical_harvest_period_variety",
        columnNames = {"\"year\"", "\"month\"", "\"week\"", "variety_key"}),
        indexes = @Index(name = "idx_historical_harvest_variety_key", columnList = "variety_key"))
@Getter
@Setter
@ToString
@NoArgsConstructor
public class HistoricalHarvest extends BaseEntity {

    @Column(name = "\"year\"", nullable = false)
    private Integer year;

    @Column(name = "\"month\"", nullable = false)
    private Integer month;

    @Column(name = "\"week\"", nullable = false)
    private Integer week;

    /** Catalogue key of the variety, for example {@code KENT}: foreign key to plants {@code variety_reference}. */
    @Column(name = "variety_key", nullable = false, length = VarietyCatalog.KEY_LENGTH)
    private String varietyKey;

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

}