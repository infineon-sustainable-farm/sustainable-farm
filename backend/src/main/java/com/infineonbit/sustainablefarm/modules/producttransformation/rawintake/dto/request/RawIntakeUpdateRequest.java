package com.infineonbit.sustainablefarm.modules.producttransformation.rawintake.dto.request;

import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.QualityGrade;
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

    /** Variety: KEITT, KENT, TOMMY, AMÉLIE, OTHER or another key of the plants variety catalogue. */
    private String receivedVariety;

    private QualityGrade receivedGrade;

    /** Person of the people directory (GET /api/staff/persons) who received the lot, or {@code null}. */
    private Long intakeOperatorPersonId;
}
