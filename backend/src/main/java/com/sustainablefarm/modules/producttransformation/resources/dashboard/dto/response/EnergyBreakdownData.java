package com.sustainablefarm.modules.producttransformation.resources.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Energy Breakdown Chart Data
 * Represents energy consumption breakdown for pie chart
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnergyBreakdownData {
    
    private String name;              // Energy source name
    private BigDecimal value;         // Energy value in kWh
    
    /**
     * Get value as double for charting libraries
     */
    public double getValue() {
        return value != null ? value.doubleValue() : 0.0;
    }
}