package com.sustainablefarm.modules.producttransformation.resources.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Production Output Chart Data
 * Represents production output data for charting
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionOutputData {
    
    private String period;           // Period (e.g., "Week 1", "Week 2", "Week 3", "Week 4")
    private BigDecimal output;        // Production output in kg
    
    /**
     * Get output as double for charting libraries
     */
    public double getOutputValue() {
        return output != null ? output.doubleValue() : 0.0;
    }
}