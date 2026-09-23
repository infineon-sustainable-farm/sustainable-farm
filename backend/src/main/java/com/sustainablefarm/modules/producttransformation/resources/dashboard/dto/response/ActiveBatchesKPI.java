package com.sustainablefarm.modules.producttransformation.resources.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Active Batches KPI
 * Represents the current active batches in production
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveBatchesKPI {
    
    private Integer activeBatchCount;          // Number of active batches
    private Integer totalBatchCount;          // Total number of batches
    private BigDecimal activePercentage;      // Active batch percentage (%)
    private String status;                    // "production", "idle", "error"
    private LocalDate asOfDate;              // As of date
    private List<BatchSummary> batchSummaries; // Batch summaries
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchSummary {
        private String batchId;
        private String productName;
        private String currentStage;
        private BigDecimal quantity;
        private LocalDate startDate;
        private String status;
    }
}