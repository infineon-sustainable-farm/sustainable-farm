package com.infineonbit.sustainablefarm.modules.producttransformation.rawintake.dto.request;

import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent.MangoVariety;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent.QualityGrade;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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
    private BigDecimal receivedQuantityKg;

    private MangoVariety receivedVariety;

    private QualityGrade receivedGrade;

    private String intakeOperator;
}
