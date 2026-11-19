package com.infineonbit.sustainablefarm.modules.producttransformation.rawintake.dto.response;

import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.QualityGrade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;

/**
 * DTO for RawIntake response
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawIntakeResponse {

    private String intakeId;
    private String batchId;
    private String sourceFarm;
    private String sourceBlock;
    private LocalDate intakeDate;
    private BigDecimal receivedQuantityKg;
    /** Variety: KEITT, KENT, TOMMY, AMÉLIE, OTHER or another key of the plants variety catalogue. */
    private String receivedVariety;
    private QualityGrade receivedGrade;
    private String intakeOperator;
    private Long intakeOperatorPersonId;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
