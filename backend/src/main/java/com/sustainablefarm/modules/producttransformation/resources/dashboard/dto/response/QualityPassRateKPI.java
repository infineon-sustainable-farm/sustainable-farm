package com.sustainablefarm.modules.producttransformation.resources.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Quality Pass Rate KPI
 * Represents the quality pass rate for quality control checkpoints
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityPassRateKPI {
    
    private BigDecimal passRate;              // Quality pass rate (%)
    private Integer totalCheckpoints;         // Total number of QC checkpoints
    private Integer passedCheckpoints;       // Number of passed checkpoints
    private Integer failedCheckpoints;       // Number of failed checkpoints
    private Integer reworkCheckpoints;       // Number of rework checkpoints
    private String targetStatus;             // "above", "on", "below" target
    private BigDecimal targetPercentage;     // Target percentage (e.g., 90%)
    private LocalDate startDate;
    private LocalDate endDate;
}