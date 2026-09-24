package com.sustainablefarm.modules.producttransformation.resources.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Harvest Trend Chart Data
 * Represents daily harvest quantity data for charting
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HarvestTrendData {
    
    private LocalDate date;           // Date of harvest
    private BigDecimal quantity;      // Harvest quantity in kg
    
    /**
     * Get quantity as double for charting libraries
     */
    public double getQuantityValue() {
        return quantity != null ? quantity.doubleValue() : 0.0;
    }
}