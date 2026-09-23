package com.sustainablefarm.modules.producttransformation.resources.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Quality Trend Chart Data
 * Represents quality pass rate data for trend chart
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityTrendData {
    
    private LocalDate date;           // Date of quality checkpoint
    private BigDecimal passRate;      // Pass rate percentage
    
    /**
     * Get passRate as double for charting libraries
     */
    public double getPassRateValue() {
        return passRate != null ? passRate.doubleValue() : 0.0;
    }
}