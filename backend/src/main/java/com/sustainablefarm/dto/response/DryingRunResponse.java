package com.sustainablefarm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Double durationHours;
    private Double targetTemperatureC;
    private Double actualTemperatureC;
    private Double startMoisturePct;
    private Double endMoisturePct;
    private Double energyUsageKwh;
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
