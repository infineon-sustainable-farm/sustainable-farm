package com.infineonbit.sustainablefarm.modules.producttransformation.batch.dto.response;

import com.infineonbit.sustainablefarm.modules.producttransformation.batch.model.Batch.BatchStatus;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent.MangoVariety;
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
    /**
     * Identity of the Plants harvest this batch was imported from (option B).
     * Resolved from the linked harvest_event (joined by batch_id), never
     * duplicated into the batch table. Null for batches that do not come
     * from the Plants import.
     */
    private String lotNumber;
    private Long plantsHarvestId;
}
