package com.sustainablefarm.dto.request;

import com.sustainablefarm.model.Batch.BatchStatus;
import com.sustainablefarm.model.HarvestEvent.MangoVariety;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for creating a new Batch
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchCreateRequest {

    @NotBlank(message = "Batch ID is required")
    private String batchId;

    @NotNull(message = "Harvest date is required")
    private LocalDate harvestDate;

    @NotNull(message = "Mango variety is required")
    private MangoVariety mangoVariety;

    @NotNull(message = "Harvest quantity is required")
    @Positive(message = "Harvest quantity must be positive")
    private Double harvestQuantityKg;

    @NotBlank(message = "Farm ID is required")
    private String farmId;

    @NotBlank(message = "Block ID is required")
    private String blockId;

    private BatchStatus currentStatus;
}
