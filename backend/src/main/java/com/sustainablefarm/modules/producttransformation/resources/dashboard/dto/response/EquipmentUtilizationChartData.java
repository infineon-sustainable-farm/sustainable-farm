package com.sustainablefarm.modules.producttransformation.resources.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Equipment Utilization Chart Data
 * Represents equipment utilization data for bar chart
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentUtilizationChartData {
    
    private String equipment;         // Equipment name
    private BigDecimal utilization;   // Utilization percentage
    
    /**
     * Get utilization as double for charting libraries
     */
    public double getUtilizationValue() {
        return utilization != null ? utilization.doubleValue() : 0.0;
    }
}