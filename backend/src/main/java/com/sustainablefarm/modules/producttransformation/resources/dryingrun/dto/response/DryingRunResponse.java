package com.sustainablefarm.modules.producttransformation.resources.dryingrun.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * DTO for DryingRun response
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DryingRunResponse {

    private String runId;
    private String batchId;
    private BigDecimal durationHours;
    private BigDecimal targetTemperatureC;
    private BigDecimal actualTemperatureC;
    private BigDecimal startMoisturePct;
    private BigDecimal endMoisturePct;
    private BigDecimal energyUsageKwh;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String equipmentId;
    private String operatorId;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    
    // Computed fields
    private Double moistureReductionPct;
    private Double energyEfficiencyKwhPerKg;
    private Boolean withinTargetRange;
}
