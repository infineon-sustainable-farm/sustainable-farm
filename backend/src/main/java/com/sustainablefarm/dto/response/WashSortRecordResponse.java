package com.sustainablefarm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * DTO for WashSortRecord response
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WashSortRecordResponse {

    private String recordId;
    private String batchId;
    private Double inputQuantityKg;
    private Double outputQuantityKg;
    private Double wasteQuantityKg;
    private Double waterUsageLiters;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String equipmentId;
    private String operatorId;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    
    // Computed fields
    private Double yieldPercentage;
    private Double wastePercentage;
}
