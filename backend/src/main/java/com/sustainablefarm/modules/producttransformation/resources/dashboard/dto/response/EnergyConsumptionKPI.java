package com.sustainablefarm.modules.producttransformation.resources.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Energy Consumption KPI
 * Represents energy consumption metrics for drying operations
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnergyConsumptionKPI {
    
    private BigDecimal averageConsumption;     // Average energy consumption (kWh/kg)
    private BigDecimal totalConsumption;       // Total energy consumption (kWh)
    private BigDecimal solarConsumption;      // Solar energy consumption (kWh)
    private BigDecimal gridConsumption;       // Grid energy consumption (kWh)
    private BigDecimal solarShare;            // Solar energy share (%)
    private String efficiencyStatus;         // "on_target", "above", "below"
    private BigDecimal targetConsumption;     // Target consumption (kWh/kg)
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer dryingRunCount;           // Number of drying runs
}