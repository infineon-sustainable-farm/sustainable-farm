package com.sustainablefarm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Dashboard KPI Response
 * Aggregated dashboard metrics for the Product Transformation application
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardKPIResponse {
    
    // Operational KPIs
    private BigDecimal harvestQuantity;        // Total harvest quantity (kg)
    private Integer activeBatches;             // Number of active batches
    private BigDecimal productionOutput;        // Total production output (kg)
    private BigDecimal qualityPassRate;         // Quality pass rate (%)
    
    // Resource KPIs
    private BigDecimal waterConsumption;        // Water consumption (L/kg)
    private BigDecimal energyConsumption;       // Energy consumption (kWh/kg)
    private BigDecimal solarEnergyShare;        // Solar energy share (%)
    private BigDecimal equipmentUtilization;    // Equipment utilization (%)
    
    // Quality KPIs
    private BigDecimal gradeAPercentage;        // Grade A percentage (%)
    private String qualityTargetStatus;        // Quality target status
    
    // Production Analytics KPIs
    private BigDecimal totalEnergyToday;        // Total energy today (kWh)
    private BigDecimal productionEfficiency;    // Production efficiency (%)
    
    // Metadata
    private LocalDate startDate;
    private LocalDate endDate;
    private String period;                     // "day", "week", "month"
    private String generatedAt;              // Timestamp of KPI generation
    
    /**
     * Check if all KPIs have valid values
     */
    public boolean isComplete() {
        return harvestQuantity != null 
            && activeBatches != null 
            && qualityPassRate != null 
            && equipmentUtilization != null;
    }
    
    /**
     * Check if the quality target is met
     */
    public boolean isQualityTargetMet() {
        return gradeAPercentage != null && gradeAPercentage.compareTo(new BigDecimal("75")) >= 0;
    }
}