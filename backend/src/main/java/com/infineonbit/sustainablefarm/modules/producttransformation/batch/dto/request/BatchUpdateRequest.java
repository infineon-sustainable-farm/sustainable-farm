package com.infineonbit.sustainablefarm.modules.producttransformation.batch.dto.request;

import com.infineonbit.sustainablefarm.modules.producttransformation.batch.model.Batch.BatchStatus;
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

    /** Variety: KEITT, KENT, TOMMY, AMÉLIE, OTHER or another key of the plants variety catalogue. */
    private String mangoVariety;

    @Positive(message = "Harvest quantity must be positive")
    private BigDecimal harvestQuantityKg;

    private String farmId;

    private String blockId;

    private BatchStatus currentStatus;
}
