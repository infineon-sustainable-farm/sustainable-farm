package com.sustainablefarm.modules.producttransformation.resources.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Harvest Quantity KPI
 * Represents the total harvest quantity for a given period
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HarvestQuantityKPI {
    
    private BigDecimal totalQuantity;           // Total harvest quantity (kg)
    private BigDecimal changePercentage;       // Change vs previous period (%)
    private String trend;                      // "up", "down", "stable"
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer harvestEventCount;         // Number of harvest events
}