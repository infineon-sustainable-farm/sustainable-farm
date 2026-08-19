package com.sustainablefarm.dto.response;

import com.sustainablefarm.model.Batch.BatchStatus;
import com.sustainablefarm.model.HarvestEvent.MangoVariety;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;

/**
 * DTO for Batch response
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchResponse {

    private String batchId;
    private LocalDate harvestDate;
    private MangoVariety mangoVariety;
    private BigDecimal harvestQuantityKg;
    private BatchStatus currentStatus;
    private String farmId;
    private String blockId;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
