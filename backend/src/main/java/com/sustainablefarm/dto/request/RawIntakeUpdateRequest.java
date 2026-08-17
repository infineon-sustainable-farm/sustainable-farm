package com.sustainablefarm.dto.request;

import com.sustainablefarm.model.HarvestEvent.MangoVariety;
import com.sustainablefarm.model.HarvestEvent.QualityGrade;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for updating an existing RawIntake
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RawIntakeUpdateRequest {

    private String sourceFarm;

    private String sourceBlock;

    private LocalDate intakeDate;

    @Positive(message = "Received quantity must be positive")
    private Double receivedQuantityKg;

    private MangoVariety receivedVariety;

    private QualityGrade receivedGrade;

    private String intakeOperator;
}
