package com.sustainablefarm.modules.producttransformation.resources.batch.dto.request;

import com.sustainablefarm.modules.producttransformation.resources.batch.model.Batch.BatchStatus;
import com.sustainablefarm.modules.producttransformation.resources.harvestevent.model.HarvestEvent.MangoVariety;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for updating an existing Batch
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchUpdateRequest {

    private LocalDate harvestDate;

    private MangoVariety mangoVariety;

    @Positive(message = "Harvest quantity must be positive")
    private BigDecimal harvestQuantityKg;

    private String farmId;

    private String blockId;

    private BatchStatus currentStatus;
}
