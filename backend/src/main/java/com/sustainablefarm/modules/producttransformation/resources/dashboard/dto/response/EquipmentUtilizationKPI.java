package com.sustainablefarm.modules.producttransformation.resources.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Equipment Utilization KPI
 * Represents equipment utilization metrics
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentUtilizationKPI {
    
    private BigDecimal utilizationRate;       // Equipment utilization rate (%)
    private Integer totalEquipment;          // Total number of equipment
    private Integer activeEquipment;         // Number of active equipment
    private Integer maintenanceEquipment;    // Number of equipment under maintenance
    private Integer inactiveEquipment;       // Number of inactive equipment
    private String overallStatus;           // "operational", "degraded", "critical"
    private LocalDate asOfDate;             // As of date
}