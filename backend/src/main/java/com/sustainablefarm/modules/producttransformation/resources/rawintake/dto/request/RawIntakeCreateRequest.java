package com.sustainablefarm.modules.producttransformation.resources.rawintake.dto.request;

import com.sustainablefarm.modules.producttransformation.resources.harvestevent.model.HarvestEvent.MangoVariety;
import com.sustainablefarm.modules.producttransformation.resources.harvestevent.model.HarvestEvent.QualityGrade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating a new RawIntake
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RawIntakeCreateRequest {

    @NotBlank(message = "Intake ID is required")
    private String intakeId;

    @NotBlank(message = "Batch ID is required")
    private String batchId;

    @NotBlank(message = "Source farm is required")
    private String sourceFarm;

    @NotBlank(message = "Source block is required")
    private String sourceBlock;

    @NotNull(message = "Intake date is required")
    private LocalDate intakeDate;

    @NotNull(message = "Received quantity is required")
    @Positive(message = "Received quantity must be positive")
    private BigDecimal receivedQuantityKg;

    @NotNull(message = "Received variety is required")
    private MangoVariety receivedVariety;

    @NotNull(message = "Received grade is required")
    private QualityGrade receivedGrade;

    private String intakeOperator;
}
